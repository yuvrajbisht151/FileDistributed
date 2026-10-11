package com.example.dfs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One chunk of a file. Each chunk has a primary copy on one node and a replica copy on a
 * different node. The stored names are generated UUID-based filenames, never user input.
 */
@Entity
@Table(name = "file_chunks",
        uniqueConstraints = @UniqueConstraint(name = "uk_chunk_file_index", columnNames = {"file_id", "chunk_index"}),
        indexes = {
                @Index(name = "idx_chunks_primary_node", columnList = "primary_node_id"),
                @Index(name = "idx_chunks_replica_node", columnList = "replica_node_id")
        })
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_size", nullable = false)
    private int chunkSize;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @ManyToOne(optional = false)
    @JoinColumn(name = "primary_node_id", nullable = false)
    private StorageNode primaryNode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "replica_node_id", nullable = false)
    private StorageNode replicaNode;

    @Column(name = "primary_stored_name", nullable = false, length = 64)
    private String primaryStoredName;

    @Column(name = "replica_stored_name", nullable = false, length = 64)
    private String replicaStoredName;

    public Long getId() { return id; }
    public StoredFile getFile() { return file; }
    public void setFile(StoredFile file) { this.file = file; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }
    public StorageNode getPrimaryNode() { return primaryNode; }
    public void setPrimaryNode(StorageNode primaryNode) { this.primaryNode = primaryNode; }
    public StorageNode getReplicaNode() { return replicaNode; }
    public void setReplicaNode(StorageNode replicaNode) { this.replicaNode = replicaNode; }
    public String getPrimaryStoredName() { return primaryStoredName; }
    public void setPrimaryStoredName(String primaryStoredName) { this.primaryStoredName = primaryStoredName; }
    public String getReplicaStoredName() { return replicaStoredName; }
    public void setReplicaStoredName(String replicaStoredName) { this.replicaStoredName = replicaStoredName; }
}
