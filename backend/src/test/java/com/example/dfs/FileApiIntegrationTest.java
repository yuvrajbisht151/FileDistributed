package com.example.dfs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.dfs.entity.FileChunk;
import com.example.dfs.entity.NodeStatus;
import com.example.dfs.entity.StorageNode;
import com.example.dfs.repository.ChunkMetadataRepository;
import com.example.dfs.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileApiIntegrationTest {

    private static final int CHUNK = 1_048_576;

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private StorageService storageService;
    @Autowired private ChunkMetadataRepository chunkRepository;

    @BeforeEach
    void allNodesOnline() {
        for (StorageNode n : storageService.findAllNodes()) {
            storageService.changeNodeStatus(n.getId(), NodeStatus.ONLINE);
        }
    }

    @Test
    void uploadThenDownloadReturnsIdenticalBytes() throws Exception {
        byte[] data = randomBytes(2_500_000);
        long id = upload("report.bin", data);

        mvc.perform(get("/api/files/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file.chunkCount").value(3));

        assertArrayEquals(data, download(id));
    }

    @Test
    void everyChunkHasPrimaryAndReplicaOnDifferentNodes() throws Exception {
        long id = upload("split.bin", randomBytes(3 * CHUNK + 10));

        JsonNode chunks = mapper.readTree(fileDetailJson(id)).get("chunks");
        assertEquals(4, chunks.size());
        for (JsonNode c : chunks) {
            assertNotEquals(c.get("primaryNode").asText(), c.get("replicaNode").asText());
        }
    }

    @Test
    void downloadFallsBackToReplicaWhenPrimaryNodeIsOffline() throws Exception {
        byte[] data = randomBytes(2_500_000);
        long id = upload("fallback.bin", data);
        StorageNode node1 = nodeNamed("node1");

        storageService.changeNodeStatus(node1.getId(), NodeStatus.OFFLINE);
        try {
            assertArrayEquals(data, download(id));
        } finally {
            storageService.changeNodeStatus(node1.getId(), NodeStatus.ONLINE);
        }
    }

    @Test
    void corruptedPrimaryIsReportedAndReplicaKeepsDownloadCorrect() throws Exception {
        byte[] data = randomBytes(1_200_000);
        long id = upload("corrupt.bin", data);

        FileChunk first = chunkRepository.findByFileIdOrderByChunkIndexAsc(id).get(0);
        Path primary = storageService.resolveChunkPath(first.getPrimaryNode(), first.getPrimaryStoredName());
        Files.write(primary, "tampered".getBytes());

        assertArrayEquals(data, download(id));

        MvcResult result = mvc.perform(post("/api/files/{id}/verify", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRUPTED"))
                .andReturn();
        JsonNode report = mapper.readTree(result.getResponse().getContentAsString());
        assertEquals("CORRUPTED", report.get("chunks").get(0).get("status").asText());
        assertEquals("CORRUPTED", report.get("chunks").get(0).get("primaryStatus").asText());
    }

    @Test
    void verifyReportsVerifiedForHealthyFile() throws Exception {
        long id = upload("healthy.bin", randomBytes(CHUNK + 5));
        mvc.perform(post("/api/files/{id}/verify", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.overallChecksumMatches").value(true));
    }

    @Test
    void deleteRemovesChunkFilesAndMetadata() throws Exception {
        long id = upload("delete-me.bin", randomBytes(2 * CHUNK));
        List<FileChunk> chunks = chunkRepository.findByFileIdOrderByChunkIndexAsc(id);
        List<Path> paths = chunks.stream().flatMap(c -> java.util.stream.Stream.of(
                storageService.resolveChunkPath(c.getPrimaryNode(), c.getPrimaryStoredName()),
                storageService.resolveChunkPath(c.getReplicaNode(), c.getReplicaStoredName()))).toList();
        for (Path p : paths) {
            assertTrue(Files.exists(p));
        }

        mvc.perform(delete("/api/files/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        for (Path p : paths) {
            assertFalse(Files.exists(p));
        }
        assertTrue(chunkRepository.findByFileIdOrderByChunkIndexAsc(id).isEmpty());
        mvc.perform(get("/api/files/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteWhileNodeOfflineReportsPendingCleanup() throws Exception {
        long id = upload("pending.bin", randomBytes(CHUNK));
        List<StorageNode> nodes = storageService.findAllNodes();
        // Every copy lives on some node, so taking all nodes offline guarantees the delete is incomplete.
        nodes.forEach(n -> storageService.changeNodeStatus(n.getId(), NodeStatus.OFFLINE));
        try {
            mvc.perform(delete("/api/files/{id}", id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.deleted").value(false))
                    .andExpect(jsonPath("$.fileStatus").value("PENDING_CLEANUP"));
        } finally {
            nodes.forEach(n -> storageService.changeNodeStatus(n.getId(), NodeStatus.ONLINE));
        }
        mvc.perform(delete("/api/files/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void nodeOfflineAndOnlineEndpointsUpdatePersistedStatus() throws Exception {
        StorageNode node = nodeNamed("node2");
        mvc.perform(post("/api/nodes/{id}/offline", node.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFLINE"));
        mvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'node2')].status").value("OFFLINE"));

        mvc.perform(post("/api/nodes/{id}/online", node.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void offlineNodeIsNotUsedForNewUploads() throws Exception {
        StorageNode node3 = nodeNamed("node3");
        storageService.changeNodeStatus(node3.getId(), NodeStatus.OFFLINE);
        try {
            long id = upload("avoid-offline.bin", randomBytes(3 * CHUNK));
            for (JsonNode c : mapper.readTree(fileDetailJson(id)).get("chunks")) {
                assertNotEquals("node3", c.get("primaryNode").asText());
                assertNotEquals("node3", c.get("replicaNode").asText());
            }
        } finally {
            storageService.changeNodeStatus(node3.getId(), NodeStatus.ONLINE);
        }
    }

    @Test
    void uploadFailsWhenFewerThanTwoNodesAreOnline() throws Exception {
        StorageNode a = nodeNamed("node1");
        StorageNode b = nodeNamed("node2");
        storageService.changeNodeStatus(a.getId(), NodeStatus.OFFLINE);
        storageService.changeNodeStatus(b.getId(), NodeStatus.OFFLINE);
        try {
            mvc.perform(multipart("/api/files/upload")
                            .file(new MockMultipartFile("file", "x.bin", "application/octet-stream", new byte[10])))
                    .andExpect(status().isServiceUnavailable());
        } finally {
            storageService.changeNodeStatus(a.getId(), NodeStatus.ONLINE);
            storageService.changeNodeStatus(b.getId(), NodeStatus.ONLINE);
        }
    }

    @Test
    void emptyUploadIsRejected() throws Exception {
        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dashboardStatsReflectUploads() throws Exception {
        upload("stats.bin", randomBytes(CHUNK));
        mvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes").value(3))
                .andExpect(jsonPath("$.totalFiles").isNumber());
    }

    // ---- helpers -------------------------------------------------------------

    private long upload(String name, byte[] data) throws Exception {
        MvcResult result = mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", name, "application/octet-stream", data)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private byte[] download(long id) throws Exception {
        return mvc.perform(get("/api/files/{id}/download", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String fileDetailJson(long id) throws Exception {
        return mvc.perform(get("/api/files/{id}", id)).andReturn().getResponse().getContentAsString();
    }

    private StorageNode nodeNamed(String name) {
        return storageService.findAllNodes().stream()
                .filter(n -> n.getName().equals(name)).findFirst().orElseThrow();
    }

    private static byte[] randomBytes(int size) {
        byte[] data = new byte[size];
        new Random(42).nextBytes(data);
        return data;
    }
}
