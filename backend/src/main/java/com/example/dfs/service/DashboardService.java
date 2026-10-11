package com.example.dfs.service;

import com.example.dfs.dto.DashboardStatsDto;
import com.example.dfs.dto.FileDto;
import com.example.dfs.entity.NodeStatus;
import com.example.dfs.entity.StorageNode;
import com.example.dfs.repository.ChunkMetadataRepository;
import com.example.dfs.repository.FileMetadataRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds dashboard numbers from the database, never from hardcoded values. */
@Service
public class DashboardService {

    private final FileMetadataRepository fileRepository;
    private final ChunkMetadataRepository chunkRepository;
    private final StorageService storageService;

    public DashboardService(FileMetadataRepository fileRepository, ChunkMetadataRepository chunkRepository,
                            StorageService storageService) {
        this.fileRepository = fileRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto stats() {
        List<StorageNode> nodes = storageService.findAllNodes();
        long primary = 0;
        long replica = 0;
        for (StorageNode n : nodes) {
            StorageService.NodeStats s = storageService.nodeStats(n);
            primary += s.primaryChunks();
            replica += s.replicaChunks();
        }
        int online = (int) nodes.stream().filter(n -> n.getStatus() == NodeStatus.ONLINE).count();
        List<FileDto> recent = fileRepository.findTop5ByOrderByUploadedAtDesc()
                .stream().map(FileDto::from).toList();

        Long logical = fileRepository.sumFileSizes();
        return new DashboardStatsDto(
                fileRepository.count(),
                chunkRepository.count(),
                logical == null ? 0L : logical,
                storageService.totalPhysicalBytes(),
                nodes.size(),
                online,
                primary,
                replica,
                recent);
    }
}
