package com.example.dfs.controller;

import com.example.dfs.dto.DeleteResultDto;
import com.example.dfs.dto.FileDetailDto;
import com.example.dfs.dto.FileDto;
import com.example.dfs.dto.IntegrityReportDto;
import com.example.dfs.service.FileService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.upload(file));
    }

    @GetMapping
    public List<FileDto> list(@RequestParam(value = "search", required = false) String search) {
        return fileService.list(search);
    }

    @GetMapping("/{id}")
    public FileDetailDto get(@PathVariable("id") Long id) {
        return fileService.detail(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") Long id) {
        FileService.Download download = fileService.openDownload(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.size())
                .body(new InputStreamResource(download.stream()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteResultDto> delete(@PathVariable("id") Long id) {
        DeleteResultDto result = fileService.delete(id);
        return result.deleted() ? ResponseEntity.ok(result) : ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    @PostMapping("/{id}/verify")
    public IntegrityReportDto verify(@PathVariable("id") Long id) {
        return fileService.verify(id);
    }
}
