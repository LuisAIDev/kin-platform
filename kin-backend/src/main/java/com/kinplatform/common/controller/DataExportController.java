package com.kinplatform.common.controller;

import com.kinplatform.common.entity.DataExportRequest;
import com.kinplatform.common.service.DataExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/data-export")
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'PHYSICIAN', 'IPS_ADMIN')")
    public ResponseEntity<DataExportRequest> requestExport(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.requestExport(userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'PHYSICIAN', 'IPS_ADMIN')")
    public ResponseEntity<List<DataExportRequest>> listExports(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(service.getExportsByUser(userId));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PATIENT', 'PHYSICIAN', 'IPS_ADMIN')")
    public ResponseEntity<DataExportRequest> getStatus(@PathVariable UUID id, Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(service.getExportStatus(id, userId));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('PATIENT', 'PHYSICIAN', 'IPS_ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, Authentication auth) throws IOException {
        UUID userId = extractUserId(auth);
        byte[] bytes = service.downloadExport(id, userId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data-export-" + id + ".zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes);
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}