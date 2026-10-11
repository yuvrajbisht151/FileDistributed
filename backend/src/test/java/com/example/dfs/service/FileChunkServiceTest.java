package com.example.dfs.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class FileChunkServiceTest {

    private final FileChunkService chunks = new FileChunkService();

    @Test
    void countChunksRoundsUpAndHandlesEmptyFiles() {
        assertEquals(0, chunks.countChunks(0, 1024));
        assertEquals(1, chunks.countChunks(1, 1024));
        assertEquals(1, chunks.countChunks(1024, 1024));
        assertEquals(2, chunks.countChunks(1025, 1024));
        assertEquals(3, chunks.countChunks(2_621_440, 1_048_576));
    }

    @Test
    void splittingAndReassemblingReturnsOriginalBytes() throws IOException {
        byte[] original = new byte[2_500_000];
        new java.util.Random(7).nextBytes(original);

        int chunkSize = 1_048_576;
        ByteArrayOutputStream rebuilt = new ByteArrayOutputStream();
        int count = 0;
        try (InputStream in = new ByteArrayInputStream(original)) {
            byte[] piece;
            while ((piece = chunks.readNextChunk(in, chunkSize)).length > 0) {
                assertTrue(piece.length <= chunkSize);
                rebuilt.write(piece);
                count++;
            }
        }

        assertEquals(chunks.countChunks(original.length, chunkSize), count);
        assertArrayEquals(original, rebuilt.toByteArray());
    }

    @Test
    void lastChunkCanBeSmallerThanChunkSize() throws IOException {
        byte[] data = new byte[2500];
        try (InputStream in = new ByteArrayInputStream(data)) {
            assertEquals(1000, chunks.readNextChunk(in, 1000).length);
            assertEquals(1000, chunks.readNextChunk(in, 1000).length);
            assertEquals(500, chunks.readNextChunk(in, 1000).length);
            assertEquals(0, chunks.readNextChunk(in, 1000).length);
        }
    }

    @Test
    void matchesChecksumDetectsChangedBytes() {
        byte[] data = "hello".getBytes();
        String expected = com.example.dfs.util.ChecksumUtil.sha256Hex(data);
        assertTrue(chunks.matchesChecksum(data, expected));
        assertFalse(chunks.matchesChecksum("hellO".getBytes(), expected));
    }
}
