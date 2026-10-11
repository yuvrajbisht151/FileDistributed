package com.example.dfs.dto;

import com.example.dfs.entity.FileStatus;
import com.example.dfs.entity.StoredFile;
import java.time.Instant;

public record FileDto(
        Long id,
        String originalFilename,
        long fileSize,
        String checksumSha256,
        int chunkCount,
        int chunkSize,
        Instant uploadedAt,
        FileStatus status) {

    public static FileDto from(StoredFile f) {
        return new FileDto(f.getId(), f.getOriginalFilename(), f.getFileSize(), f.getChecksumSha256(),
                f.getChunkCount(), f.getChunkSize(), f.getUploadedAt(), f.getStatus());
    }
}
