package com.example.dfs.controller;

import com.example.dfs.dto.NodeDto;
import com.example.dfs.entity.NodeStatus;
import com.example.dfs.service.StorageService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class StorageNodeController {

    private final StorageService storageService;

    public StorageNodeController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public List<NodeDto> list() {
        return storageService.listNodes();
    }

    @GetMapping("/{id}")
    public NodeDto get(@PathVariable("id") Long id) {
        return storageService.getNodeDto(id);
    }

    @PostMapping("/{id}/offline")
    public NodeDto offline(@PathVariable("id") Long id) {
        return storageService.changeNodeStatus(id, NodeStatus.OFFLINE);
    }

    @PostMapping("/{id}/online")
    public NodeDto online(@PathVariable("id") Long id) {
        return storageService.changeNodeStatus(id, NodeStatus.ONLINE);
    }
}
