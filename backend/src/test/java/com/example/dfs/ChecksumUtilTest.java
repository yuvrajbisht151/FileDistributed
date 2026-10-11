package com.example.dfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.dfs.util.ChecksumUtil;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ChecksumUtilTest {

    @Test
    void sha256OfAbcMatchesKnownVector() {
        String hash = ChecksumUtil.sha256Hex("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash);
    }

    @Test
    void sha256OfEmptyInputMatchesKnownVector() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                ChecksumUtil.sha256Hex(new byte[0]));
    }
}
