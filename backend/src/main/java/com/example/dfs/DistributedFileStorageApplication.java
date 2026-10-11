package com.example.dfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DistributedFileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedFileStorageApplication.class, args);
    }
}
