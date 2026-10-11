package com.example.dfs.config;

import com.example.dfs.service.StorageService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Runs at startup: makes sure the logical storage nodes exist in the DB and their folders exist on disk. */
@Component
public class NodeInitializer implements ApplicationRunner {

    private final StorageService storageService;

    public NodeInitializer(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        storageService.initializeNodes();
    }
}
