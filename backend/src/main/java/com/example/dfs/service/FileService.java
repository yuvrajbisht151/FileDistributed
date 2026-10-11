package com.example.dfs.service;

import com.example.dfs.config.StorageProperties;
import com.example.dfs.dto.ChunkDto;
import com.example.dfs.dto.ChunkIntegrityDto;
import com.example.dfs.dto.DeleteResultDto;
import com.example.dfs.dto.FileDetailDto;
import com.example.dfs.dto.FileDto;
import com.example.dfs.dto.IntegrityReportDto;
import com.example.dfs.entity.FileChunk;
import com.example.dfs.entity.FileStatus;
import com.example.dfs.entity.StorageNode;
import com.example.dfs.entity.StoredFile;
import com.example.dfs.exception.BadRequestException;
import com.example.dfs.exception.ResourceNotFoundException;
import com.example.dfs.exception.ServiceUnavailableException;
import com.example.dfs.exception.StorageOperationException;
import com.example.dfs.repository.ChunkMetadataRepository;
import com.example.dfs.repository.FileMetadataRepository;
import com.example.dfs.service.ReplicationService.ChunkPlacement;
import com.example.dfs.service.ReplicationService.CopyResult;
import com.example.dfs.service.ReplicationService.CopyStatus;
import com.example.dfs.util.ChecksumUtil;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Business logic for upload, list, download, integrity verification and delete. */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    /** Result of preparing a download. The stream deletes its temp file when it is closed. */
    public record Download(InputStream stream, long size, String filename) {
    }

    private final FileMetadataRepository fileRepository;
    private final ChunkMetadataRepository chunkRepository;
    private final StorageService storageService;
    private final ReplicationService replication;
    private final FileChunkService chunkService;
    private final StorageProperties props;
    private final TransactionTemplate tx;

    public FileService(FileMetadataRepository fileRepository, ChunkMetadataRepository chunkRepository,
                       StorageService storageService, ReplicationService replication,
                       FileChunkService chunkService, StorageProperties props, TransactionTemplate tx) {
        this.fileRepository = fileRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
        this.replication = replication;
        this.chunkService = chunkService;
        this.props = props;
        this.tx = tx;
    }

    /** Splits the upload into chunks, writes primary + replica copies, then saves metadata. */
    public FileDto upload(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new BadRequestException("Please choose a non-empty file to upload.");
        }
        List<StorageNode> online = storageService.findOnlineNodes();
        if (online.size() < 2) {
            throw new ServiceUnavailableException(
                    "At least two storage nodes must be online to store a replicated file. Online now: " + online.size());
        }

        int chunkSize = props.chunkSizeBytes();
        int startOffset = (int) (chunkRepository.count() % online.size());
        List<Path> written = new ArrayList<>();
        List<FileChunk> chunks = new ArrayList<>();
        MessageDigest overall = ChecksumUtil.newSha256();
        long totalBytes = 0;

        try (InputStream in = upload.getInputStream()) {
            int index = 0;
            byte[] data;
            while ((data = chunkService.readNextChunk(in, chunkSize)).length > 0) {
                overall.update(data);
                totalBytes += data.length;

                ChunkPlacement placement = replication.place(online, index, startOffset);
                String primaryName = replication.newStoredName();
                String replicaName = replication.newStoredName();
                replication.writeCopy(placement.primary(), primaryName, data, written);
                replication.writeCopy(placement.replica(), replicaName, data, written);

                FileChunk chunk = new FileChunk();
                chunk.setChunkIndex(index);
                chunk.setChunkSize(data.length);
                chunk.setChecksumSha256(ChecksumUtil.sha256Hex(data));
                chunk.setPrimaryNode(placement.primary());
                chunk.setReplicaNode(placement.replica());
                chunk.setPrimaryStoredName(primaryName);
                chunk.setReplicaStoredName(replicaName);
                chunks.add(chunk);
                index++;
            }
        } catch (IOException e) {
            replication.deleteQuietly(written);
            throw new StorageOperationException("Could not read the uploaded file.", e);
        } catch (RuntimeException e) {
            replication.deleteQuietly(written);
            throw e;
        }

        StoredFile file = new StoredFile();
        file.setOriginalFilename(sanitizeFilename(upload.getOriginalFilename()));
        file.setFileSize(totalBytes);
        file.setChecksumSha256(ChecksumUtil.toHex(overall.digest()));
        file.setChunkCount(chunks.size());
        file.setChunkSize(chunkSize);
        file.setUploadedAt(Instant.now());
        file.setStatus(FileStatus.STORED);

        try {
            return tx.execute(status -> {
                fileRepository.save(file);
                for (FileChunk c : chunks) {
                    c.setFile(file);
                }
                chunkRepository.saveAll(chunks);
                return FileDto.from(file);
            });
        } catch (RuntimeException e) {
            replication.deleteQuietly(written);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<FileDto> list(String search) {
        List<StoredFile> files = (search == null || search.isBlank())
                ? fileRepository.findAllByOrderByUploadedAtDesc()
                : fileRepository.findByOriginalFilenameContainingIgnoreCaseOrderByUploadedAtDesc(search.trim());
        return files.stream().map(FileDto::from).toList();
    }

    @Transactional(readOnly = true)
    public FileDetailDto detail(Long id) {
        StoredFile file = findFileOrThrow(id);
        List<ChunkDto> chunks = chunkRepository.findByFileIdOrderByChunkIndexAsc(id)
                .stream().map(ChunkDto::from).toList();
        return new FileDetailDto(FileDto.from(file), chunks);
    }

    /**
     * Rebuilds the file into a temp file, checks the overall SHA-256, and returns a stream.
     * The whole file is verified before any byte is sent, so a bad rebuild never reaches the browser.
     */
    @Transactional(readOnly = true)
    public Download openDownload(Long id) {
        StoredFile file = findFileOrThrow(id);
        List<FileChunk> chunks = chunkRepository.findByFileIdOrderByChunkIndexAsc(id);
        Path tmp = createTempFile();
        MessageDigest overall = ChecksumUtil.newSha256();
        try (OutputStream out = Files.newOutputStream(tmp)) {
            for (FileChunk c : chunks) {
                byte[] data = replication.readChunk(c).data();
                out.write(data);
                overall.update(data);
            }
        } catch (IOException e) {
            deleteTempQuietly(tmp);
            throw new StorageOperationException("Could not rebuild the file from its chunks.", e);
        } catch (RuntimeException e) {
            deleteTempQuietly(tmp);
            throw e;
        }

        if (!ChecksumUtil.toHex(overall.digest()).equals(file.getChecksumSha256())) {
            deleteTempQuietly(tmp);
            throw new StorageOperationException(
                    "The rebuilt file does not match the original checksum. Download aborted.");
        }

        InputStream stream;
        try {
            InputStream raw = Files.newInputStream(tmp);
            stream = new FilterInputStream(raw) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        Files.deleteIfExists(tmp);
                    }
                }
            };
        } catch (IOException e) {
            deleteTempQuietly(tmp);
            throw new StorageOperationException("Could not open the rebuilt file.", e);
        }
        return new Download(stream, file.getFileSize(), file.getOriginalFilename());
    }

    /**
     * Checks every chunk's primary and replica, then verifies the whole file when every chunk is readable.
     * Corrupted copies always make the result CORRUPTED, even when the other copy saves the data.
     */
    @Transactional(readOnly = true)
    public IntegrityReportDto verify(Long id) {
        StoredFile file = findFileOrThrow(id);
        List<FileChunk> chunks = chunkRepository.findByFileIdOrderByChunkIndexAsc(id);

        List<ChunkIntegrityDto> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        MessageDigest overall = ChecksumUtil.newSha256();
        boolean anyCorrupted = false;
        boolean anyUnreadable = false;

        for (FileChunk c : chunks) {
            CopyResult p = replication.readCopy(c, true);
            CopyResult r = replication.readCopy(c, false);
            boolean pOk = p.status() == CopyStatus.OK;
            boolean rOk = r.status() == CopyStatus.OK;

            String chunkStatus;
            if (pOk && rOk) {
                chunkStatus = "VERIFIED";
            } else if (p.status() == CopyStatus.CORRUPTED || r.status() == CopyStatus.CORRUPTED) {
                chunkStatus = "CORRUPTED";
            } else if (pOk || rOk) {
                chunkStatus = "DEGRADED";
            } else {
                chunkStatus = "UNAVAILABLE";
            }

            if ("CORRUPTED".equals(chunkStatus)) {
                anyCorrupted = true;
            }
            if (!pOk && !rOk) {
                anyUnreadable = true;
            }
            if (!pOk) {
                warnings.add(describeCopy("primary", c.getChunkIndex(), c.getPrimaryNode().getName(), p.status()));
            }
            if (!rOk) {
                warnings.add(describeCopy("replica", c.getChunkIndex(), c.getReplicaNode().getName(), r.status()));
            }
            if (pOk || rOk) {
                overall.update(pOk ? p.data() : r.data());
            }

            rows.add(new ChunkIntegrityDto(c.getChunkIndex(), chunkStatus,
                    c.getPrimaryNode().getName(), p.status().name(),
                    c.getReplicaNode().getName(), r.status().name()));
        }

        String status;
        Boolean matches = null;
        String message;
        if (anyCorrupted) {
            status = "CORRUPTED";
            message = "At least one stored copy failed its checksum. Corrupted data is never reported as valid.";
        } else if (anyUnreadable) {
            status = "UNAVAILABLE";
            message = "At least one chunk has no readable copy, so the file cannot be rebuilt right now.";
        } else {
            matches = ChecksumUtil.toHex(overall.digest()).equals(file.getChecksumSha256());
            status = matches ? "VERIFIED" : "CORRUPTED";
            message = matches
                    ? "Every chunk matches its checksum and the rebuilt file matches the original SHA-256."
                    : "Every chunk matches its checksum, but the rebuilt file does not match the original SHA-256.";
        }

        return new IntegrityReportDto(id, file.getOriginalFilename(), status, message, matches, rows, warnings);
    }

    /**
     * Deletes every primary and replica chunk file, then the metadata. If any copy cannot be deleted
     * (for example its node is offline), the metadata is kept with status PENDING_CLEANUP so a later
     * retry can finish the job.
     */
    public DeleteResultDto delete(Long id) {
        findFileOrThrow(id);
        List<FileChunk> chunks = chunkRepository.findByFileIdOrderByChunkIndexAsc(id);
        List<String> problems = new ArrayList<>();
        for (FileChunk c : chunks) {
            replication.deleteCopy(c, true, problems);
            replication.deleteCopy(c, false, problems);
        }

        if (problems.isEmpty()) {
            tx.executeWithoutResult(status -> {
                chunkRepository.deleteAllForFile(id);
                fileRepository.deleteById(id);
            });
            return new DeleteResultDto(id, true, null, "File and all of its copies were deleted.", List.of());
        }

        tx.executeWithoutResult(status -> {
            StoredFile f = findFileOrThrow(id);
            f.setStatus(FileStatus.PENDING_CLEANUP);
            fileRepository.save(f);
        });
        log.warn("Delete of file {} is incomplete: {}", id, problems);
        return new DeleteResultDto(id, false, FileStatus.PENDING_CLEANUP.name(),
                "Delete is incomplete. Some copies could not be removed; bring the node back online and delete again.",
                problems);
    }

    private StoredFile findFileOrThrow(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File " + id + " was not found."));
    }

    static String sanitizeFilename(String raw) {
        String name = raw == null ? "" : raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isEmpty()) {
            name = "file";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private static String describeCopy(String copy, int chunkIndex, String node, CopyStatus status) {
        String reason = switch (status) {
            case OFFLINE -> "node is offline";
            case MISSING -> "chunk file is missing";
            case CORRUPTED -> "checksum mismatch (corrupted)";
            case IO_ERROR -> "file could not be read";
            case OK -> "ok";
        };
        return "Chunk " + chunkIndex + " " + copy + " copy on " + node + ": " + reason + ".";
    }

    private static Path createTempFile() {
        try {
            return Files.createTempFile("dfs-download-", ".bin");
        } catch (IOException e) {
            throw new StorageOperationException("Could not create a temporary file for the download.", e);
        }
    }

    private static void deleteTempQuietly(Path tmp) {
        try {
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            log.warn("Could not delete temp file {}", tmp);
        }
    }
}
