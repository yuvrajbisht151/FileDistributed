package com.example.dfs.service;

import com.example.dfs.config.StorageProperties;
import com.example.dfs.dto.NodeDto;
import com.example.dfs.entity.NodeStatus;
import com.example.dfs.entity.StorageNode;
import com.example.dfs.exception.ResourceNotFoundException;
import com.example.dfs.exception.StorageOperationException;
import com.example.dfs.repository.ChunkMetadataRepository;
import com.example.dfs.repository.StorageNodeRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the logical storage nodes: their folders, availability status, and usage numbers. */
@Service
public class StorageService {

    /** Chunk files are always named like 3f2c...-uuid.chunk. Anything else is rejected. */
    private static final Pattern GENERATED_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.chunk");

    private final StorageProperties props;
    private final StorageNodeRepository nodeRepository;
    private final ChunkMetadataRepository chunkRepository;

    public StorageService(StorageProperties props, StorageNodeRepository nodeRepository,
                          ChunkMetadataRepository chunkRepository) {
        this.props = props;
        this.nodeRepository = nodeRepository;
        this.chunkRepository = chunkRepository;
    }

    /** Creates the node rows and folders if they do not exist yet. */
    @Transactional
    public void initializeNodes() {
        if (props.chunkSizeBytes() < 1024) {
            throw new IllegalStateException("app.storage.chunk-size-bytes must be at least 1024");
        }
        Path root = Path.of(props.root()).toAbsolutePath().normalize();
        for (String name : props.nodeNames()) {
            StorageNode node = nodeRepository.findByName(name).orElseGet(StorageNode::new);
            node.setName(name);
            node.setStorageDirectory(root.resolve(name).toString());
            if (node.getStatus() == null) {
                node.setStatus(NodeStatus.ONLINE);
            }
            try {
                Files.createDirectories(Path.of(node.getStorageDirectory()));
            } catch (IOException e) {
                throw new StorageOperationException("Cannot create storage folder " + node.getStorageDirectory(), e);
            }
            nodeRepository.save(node);
        }
    }

    public List<StorageNode> findAllNodes() {
        return nodeRepository.findAllByOrderByIdAsc();
    }

    public List<StorageNode> findOnlineNodes() {
        return nodeRepository.findByStatusOrderByIdAsc(NodeStatus.ONLINE);
    }

    public StorageNode getNode(Long id) {
        return nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Storage node " + id + " was not found."));
    }

    /** Simulated failure or recovery. Persisted, so it survives a backend restart. */
    @Transactional
    public NodeDto changeNodeStatus(Long id, NodeStatus status) {
        StorageNode node = getNode(id);
        node.setStatus(status);
        nodeRepository.save(node);
        return getNodeDto(id);
    }

    /** Builds a safe absolute path for a chunk file on a node. Rejects anything unexpected. */
    public Path resolveChunkPath(StorageNode node, String storedName) {
        if (storedName == null || !GENERATED_NAME.matcher(storedName).matches()) {
            throw new StorageOperationException("Invalid chunk file name in metadata: " + storedName);
        }
        Path dir = Path.of(node.getStorageDirectory()).toAbsolutePath().normalize();
        Path path = dir.resolve(storedName).normalize();
        if (!path.startsWith(dir)) {
            throw new StorageOperationException("Chunk path escapes its storage folder.");
        }
        return path;
    }

    /** Total bytes stored across all nodes, counting primary and replica copies. */
    public long totalPhysicalBytes() {
        return nz(chunkRepository.sumAllChunkBytes()) * 2;
    }

    public NodeStats nodeStats(StorageNode node) {
        return new NodeStats(
                chunkRepository.countByPrimaryNode(node),
                chunkRepository.countByReplicaNode(node),
                nz(chunkRepository.sumPrimaryBytes(node)),
                nz(chunkRepository.sumReplicaBytes(node)));
    }

    @Transactional(readOnly = true)
    public List<NodeDto> listNodes() {
        long total = totalPhysicalBytes();
        return findAllNodes().stream()
                .map(n -> toDto(n, nodeStats(n), total))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NodeDto getNodeDto(Long id) {
        StorageNode node = getNode(id);
        return toDto(node, nodeStats(node), totalPhysicalBytes());
    }

    /** Used by the dashboard to compute the same numbers for every node in one place. */
    public Map<Long, NodeStats> statsByNodeId() {
        return findAllNodes().stream().collect(Collectors.toMap(StorageNode::getId, this::nodeStats));
    }

    private NodeDto toDto(StorageNode n, NodeStats s, long totalPhysical) {
        double utilization = totalPhysical == 0 ? 0.0 : s.storedBytes() * 100.0 / totalPhysical;
        return new NodeDto(
                n.getId(),
                n.getName(),
                n.getStorageDirectory(),
                n.getStatus(),
                n.getStatus() == NodeStatus.ONLINE,
                s.primaryChunks(),
                s.replicaChunks(),
                s.primaryChunks() + s.replicaChunks(),
                s.storedBytes(),
                Math.round(utilization * 10) / 10.0);
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    /** Per-node numbers computed from the file_chunks table. */
    public record NodeStats(long primaryChunks, long replicaChunks, long primaryBytes, long replicaBytes) {
        public long storedBytes() {
            return primaryBytes + replicaBytes;
        }
    }
}
