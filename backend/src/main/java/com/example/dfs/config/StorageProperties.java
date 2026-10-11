package com.example.dfs.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings for the logical storage nodes, bound from application.yml (prefix app.storage). */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @DefaultValue("../storage") String root,
        @DefaultValue({"node1", "node2", "node3"}) List<String> nodeNames,
        @DefaultValue("1048576") int chunkSizeBytes) {
}
