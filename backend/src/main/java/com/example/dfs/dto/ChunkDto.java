package com.example.dfs.dto;

import com.example.dfs.entity.FileChunk;

public record ChunkDto(
        int chunkIndex,
        int chunkSize,
        String checksumSha256,
        String primaryNode,
        String replicaNode) {

    public static ChunkDto from(FileChunk c) {
        return new ChunkDto(c.getChunkIndex(), c.getChunkSize(), c.getChecksumSha256(),
                c.getPrimaryNode().getName(), c.getReplicaNode().getName());
    }
}
