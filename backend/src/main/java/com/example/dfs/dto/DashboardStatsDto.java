package com.example.dfs.dto;

import java.util.List;

public record DashboardStatsDto(
        long totalFiles,
        long totalChunks,
        long logicalBytes,
        long physicalBytes,
        int totalNodes,
        int onlineNodes,
        long primaryChunks,
        long replicaChunks,
        List<FileDto> recentUploads) {
}
