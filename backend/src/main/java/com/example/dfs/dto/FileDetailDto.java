package com.example.dfs.dto;

import java.util.List;

public record FileDetailDto(FileDto file, List<ChunkDto> chunks) {
}
