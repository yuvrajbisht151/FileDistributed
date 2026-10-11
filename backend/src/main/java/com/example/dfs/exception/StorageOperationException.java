package com.example.dfs.exception;

/** Unexpected failure while reading, writing, or rebuilding chunk files. */
public class StorageOperationException extends RuntimeException {

    public StorageOperationException(String message) {
        super(message);
    }

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
