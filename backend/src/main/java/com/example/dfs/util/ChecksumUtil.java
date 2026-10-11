package com.example.dfs.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Small helper for SHA-256 hashing. */
public final class ChecksumUtil {

    private ChecksumUtil() {
    }

    public static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    public static String sha256Hex(byte[] data) {
        return toHex(newSha256().digest(data));
    }

    public static String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
