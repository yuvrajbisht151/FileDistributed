package com.example.dfs.dto;

import java.util.List;

public record DeleteResultDto(
        Long fileId,
        boolean deleted,
        String fileStatus,
        String message,
        List<String> problems) {
}
