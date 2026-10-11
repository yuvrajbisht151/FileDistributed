package com.example.dfs.repository;

import com.example.dfs.entity.StoredFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FileMetadataRepository extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findAllByOrderByUploadedAtDesc();

    List<StoredFile> findByOriginalFilenameContainingIgnoreCaseOrderByUploadedAtDesc(String filename);

    List<StoredFile> findTop5ByOrderByUploadedAtDesc();

    @Query("select coalesce(sum(f.fileSize), 0) from StoredFile f")
    Long sumFileSizes();
}
