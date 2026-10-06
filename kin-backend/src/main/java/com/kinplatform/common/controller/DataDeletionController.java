package com.kinplatform.common.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kinplatform.common.entity.DataDeletionRequest;
import com.kinplatform.kin.health.legal.DataDeletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/health/data-deletion")
@RequiredArgsConstructor
public class DataDeletionController {

    private final DataDeletionService service;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<DataDeletionRequest> requestDeletion(
            Authentication auth,
            @RequestBody DeletionRequestDto dto) {
        UUID userId = extractUserId(auth);
        DataDeletionRequest request = service.requestDeletion(
            userId, dto.reason(), dto.scope(), dto.dataCategories());
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DataDeletionRequest>> getMyRequests(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(service.getRequestsByUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DataDeletionRequest>> getAllRequests(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(service.getRequestsByStatus(status));
        }
        return ResponseEntity.ok(service.getRequestsByStatus("PENDING"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataDeletionRequest> approve(
            @PathVariable UUID id,
            Authentication auth) {
        UUID adminId = extractUserId(auth);
        return ResponseEntity.ok(service.approveDeletion(id, adminId));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataDeletionRequest> reject(
            @PathVariable UUID id,
            Authentication auth,
            @RequestBody RejectRequestDto dto) {
        UUID adminId = extractUserId(auth);
        return ResponseEntity.ok(service.rejectDeletion(id, adminId, dto.reason()));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataDeletionRequest> execute(
            @PathVariable UUID id,
            @RequestBody ExecuteRequestDto dto) {
        return ResponseEntity.ok(service.executeDeletion(id, dto.confirm()));
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    public record DeletionRequestDto(
        String reason,
        String scope,
        com.fasterxml.jackson.databind.JsonNode dataCategories
    ) {}

    public record RejectRequestDto(String reason) {}

    public record ExecuteRequestDto(boolean confirm) {}
}
