package com.example.dfs.dto;

import com.example.dfs.entity.NodeStatus;

public record NodeDto(
        Long id,
        String name,
        String storageDirectory,
        NodeStatus status,
        boolean online,
        long primaryChunks,
        long replicaChunks,
        long totalChunks,
        long storedBytes,
        double utilizationPercent) {
}
