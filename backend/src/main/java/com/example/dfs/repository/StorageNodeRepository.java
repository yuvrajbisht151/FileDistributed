package com.example.dfs.repository;

import com.example.dfs.entity.NodeStatus;
import com.example.dfs.entity.StorageNode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageNodeRepository extends JpaRepository<StorageNode, Long> {

    Optional<StorageNode> findByName(String name);

    List<StorageNode> findAllByOrderByIdAsc();

    List<StorageNode> findByStatusOrderByIdAsc(NodeStatus status);
}
