package com.example.dfs.dto;

import java.util.List;

/**
 * File-level integrity result. status is VERIFIED, CORRUPTED or UNAVAILABLE.
 * overallChecksumMatches is null when the file could not be fully rebuilt.
 */
public record IntegrityReportDto(
        Long fileId,
        String filename,
        String status,
        String message,
        Boolean overallChecksumMatches,
        List<ChunkIntegrityDto> chunks,
        List<String> warnings) {
}
