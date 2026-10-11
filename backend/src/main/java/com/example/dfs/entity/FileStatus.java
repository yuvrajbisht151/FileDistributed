package com.example.dfs.entity;

/** STORED = all copies were written. PENDING_CLEANUP = a delete could not remove every copy yet. */
public enum FileStatus {
    STORED,
    PENDING_CLEANUP
}
