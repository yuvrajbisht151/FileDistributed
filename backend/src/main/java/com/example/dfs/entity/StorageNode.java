package com.example.dfs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One logical storage node (a folder such as storage/node1). Chunk counts and byte totals are
 * calculated from the file_chunks table instead of being stored here, so they can never drift.
 */
@Entity
@Table(name = "storage_nodes")
public class StorageNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "storage_directory", nullable = false, length = 500)
    private String storageDirectory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NodeStatus status = NodeStatus.ONLINE;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStorageDirectory() { return storageDirectory; }
    public void setStorageDirectory(String storageDirectory) { this.storageDirectory = storageDirectory; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
}
