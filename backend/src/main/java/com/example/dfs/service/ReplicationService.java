package com.example.dfs.service;

import com.example.dfs.entity.FileChunk;
import com.example.dfs.entity.NodeStatus;
import com.example.dfs.entity.StorageNode;
import com.example.dfs.exception.ChunkUnavailableException;
import com.example.dfs.exception.StorageOperationException;
import com.example.dfs.util.ChecksumUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Places chunk copies on nodes, reads them back with replica fallback, and deletes them.
 * Primary and replica are always on different nodes (the node list has at least two entries).
 */
@Service
public class ReplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    public enum CopyStatus { OK, OFFLINE, MISSING, CORRUPTED, IO_ERROR }

    public record ChunkPlacement(StorageNode primary, StorageNode replica) {
    }

    /** Result of reading one copy. data is only set when status is OK. */
    public record CopyResult(CopyStatus status, byte[] data) {
    }

    /** A chunk successfully read from either copy. */
    public record ChunkData(byte[] data, boolean fromReplica) {
    }

    private final StorageService storageService;

    public ReplicationService(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Round-robin placement over the online nodes: chunk i goes to node (start + i) and its replica
     * goes to the next node in rotation.
     */
    public ChunkPlacement place(List<StorageNode> online, int chunkIndex, int startOffset) {
        int n = online.size();
        StorageNode primary = online.get((startOffset + chunkIndex) % n);
        StorageNode replica = online.get((startOffset + chunkIndex + 1) % n);
        return new ChunkPlacement(primary, replica);
    }

    public String newStoredName() {
        return UUID.randomUUID() + ".chunk";
    }

    /** Writes one copy. The path is recorded in written first so a failed upload can be cleaned up. */
    public void writeCopy(StorageNode node, String storedName, byte[] data, List<Path> written) {
        Path path = storageService.resolveChunkPath(node, storedName);
        written.add(path);
        try {
            Files.write(path, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new StorageOperationException("Could not write a chunk to " + node.getName(), e);
        }
    }

    /** Reads one copy and checks its SHA-256. Offline nodes are not touched at all. */
    public CopyResult readCopy(FileChunk chunk, boolean primary) {
        StorageNode node = primary ? chunk.getPrimaryNode() : chunk.getReplicaNode();
        String storedName = primary ? chunk.getPrimaryStoredName() : chunk.getReplicaStoredName();
        if (node.getStatus() == NodeStatus.OFFLINE) {
            return new CopyResult(CopyStatus.OFFLINE, null);
        }
        Path path = storageService.resolveChunkPath(node, storedName);
        if (!Files.exists(path)) {
            return new CopyResult(CopyStatus.MISSING, null);
        }
        try {
            byte[] data = Files.readAllBytes(path);
            if (!ChecksumUtil.sha256Hex(data).equalsIgnoreCase(chunk.getChecksumSha256())) {
                log.warn("Checksum mismatch for chunk {} on {}", chunk.getChunkIndex(), node.getName());
                return new CopyResult(CopyStatus.CORRUPTED, null);
            }
            return new CopyResult(CopyStatus.OK, data);
        } catch (IOException e) {
            log.warn("Could not read chunk {} on {}: {}", chunk.getChunkIndex(), node.getName(), e.getMessage());
            return new CopyResult(CopyStatus.IO_ERROR, null);
        }
    }

    /** Returns the primary copy if it is good, otherwise the replica. */
    public ChunkData readChunk(FileChunk chunk) {
        CopyResult primary = readCopy(chunk, true);
        if (primary.status() == CopyStatus.OK) {
            return new ChunkData(primary.data(), false);
        }
        CopyResult replica = readCopy(chunk, false);
        if (replica.status() == CopyStatus.OK) {
            log.info("Chunk {} read from replica (primary was {})", chunk.getChunkIndex(), primary.status());
            return new ChunkData(replica.data(), true);
        }
        throw new ChunkUnavailableException("Chunk " + chunk.getChunkIndex() + " is unavailable: primary copy "
                + primary.status() + ", replica copy " + replica.status() + ".");
    }

    /** Deletes one copy. Returns false and records a problem if the node is offline or the delete fails. */
    public boolean deleteCopy(FileChunk chunk, boolean primary, List<String> problems) {
        StorageNode node = primary ? chunk.getPrimaryNode() : chunk.getReplicaNode();
        String storedName = primary ? chunk.getPrimaryStoredName() : chunk.getReplicaStoredName();
        String label = (primary ? "primary" : "replica") + " copy of chunk " + chunk.getChunkIndex();
        if (node.getStatus() == NodeStatus.OFFLINE) {
            problems.add(label + " is on " + node.getName() + ", which is offline");
            return false;
        }
        try {
            Files.deleteIfExists(storageService.resolveChunkPath(node, storedName));
            return true;
        } catch (IOException e) {
            problems.add("Could not delete " + label + " on " + node.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /** Best-effort removal of files written during a failed upload. */
    public void deleteQuietly(List<Path> paths) {
        for (Path p : paths) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                log.warn("Could not remove leftover chunk file {}", p);
            }
        }
    }
}
