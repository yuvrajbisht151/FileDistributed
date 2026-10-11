package com.example.dfs.repository;

import com.example.dfs.entity.FileChunk;
import com.example.dfs.entity.StorageNode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChunkMetadataRepository extends JpaRepository<FileChunk, Long> {

    List<FileChunk> findByFileIdOrderByChunkIndexAsc(Long fileId);

    long countByPrimaryNode(StorageNode node);

    long countByReplicaNode(StorageNode node);

    @Query("select coalesce(sum(c.chunkSize), 0) from FileChunk c where c.primaryNode = :node")
    Long sumPrimaryBytes(@Param("node") StorageNode node);

    @Query("select coalesce(sum(c.chunkSize), 0) from FileChunk c where c.replicaNode = :node")
    Long sumReplicaBytes(@Param("node") StorageNode node);

    @Query("select coalesce(sum(c.chunkSize), 0) from FileChunk c")
    Long sumAllChunkBytes();

    @Modifying
    @Query("delete from FileChunk c where c.file.id = :fileId")
    void deleteAllForFile(@Param("fileId") Long fileId);
}
