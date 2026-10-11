package com.example.dfs.service;

import com.example.dfs.util.ChecksumUtil;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/** Splitting logic: how many chunks a file needs and how to read the next chunk from a stream. */
@Service
public class FileChunkService {

    /** Number of chunks needed for a file (the last chunk may be smaller). */
    public int countChunks(long fileSize, int chunkSize) {
        if (fileSize <= 0) {
            return 0;
        }
        return Math.toIntExact((fileSize + chunkSize - 1) / chunkSize);
    }

    /** Reads up to chunkSize bytes. Returns an empty array when the stream is finished. */
    public byte[] readNextChunk(InputStream in, int chunkSize) throws IOException {
        return in.readNBytes(chunkSize);
    }

    /** True when the data's SHA-256 equals the expected hex value. */
    public boolean matchesChecksum(byte[] data, String expectedSha256) {
        return ChecksumUtil.sha256Hex(data).equalsIgnoreCase(expectedSha256);
    }
}
