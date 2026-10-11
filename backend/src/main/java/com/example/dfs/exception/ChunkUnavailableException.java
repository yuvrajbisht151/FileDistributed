package com.example.dfs.exception;

/** Thrown when neither the primary nor the replica copy of a chunk can be read. */
public class ChunkUnavailableException extends RuntimeException {
    public ChunkUnavailableException(String message) {
        super(message);
    }
}
