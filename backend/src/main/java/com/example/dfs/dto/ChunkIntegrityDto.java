package com.example.dfs.dto;

/**
 * Integrity result for one chunk. status is VERIFIED (both copies good), DEGRADED (one good copy),
 * CORRUPTED (a copy failed its checksum) or UNAVAILABLE (no readable copy).
 * primaryStatus / replicaStatus are OK, OFFLINE, MISSING, CORRUPTED or IO_ERROR.
 */
public record ChunkIntegrityDto(
        int chunkIndex,
        String status,
        String primaryNode,
        String primaryStatus,
        String replicaNode,
        String replicaStatus) {
}
