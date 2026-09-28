package com.kinplatform.common.controller;

import com.kinplatform.common.entity.DataRectificationRequest;
import com.kinplatform.common.service.DataRectificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/data-rectification")
@RequiredArgsConstructor
public class DataRectificationController {

    private final DataRectificationService service;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<DataRectificationRequest> requestRectification(
            Authentication auth,
            @RequestBody RectificationRequestDto dto) {
        UUID userId = extractUserId(auth);
        DataRectificationRequest request = service.requestRectification(
            userId, dto.fieldPath(), dto.oldValue(), dto.newValue(), dto.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DataRectificationRequest>> getMyRequests(Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(service.getRequestsByUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DataRectificationRequest>> getAllRequests(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(service.getRequestsByStatus(status));
        }
        return ResponseEntity.ok(service.getRequestsByStatus("PENDING"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataRectificationRequest> approve(
            @PathVariable UUID id,
            Authentication auth,
            @RequestBody ApproveRequestDto dto) {
        UUID adminId = extractUserId(auth);
        return ResponseEntity.ok(service.approveRectification(id, adminId, dto.notes()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataRectificationRequest> reject(
            @PathVariable UUID id,
            Authentication auth,
            @RequestBody RejectRequestDto dto) {
        UUID adminId = extractUserId(auth);
        return ResponseEntity.ok(service.rejectRectification(id, adminId, dto.reason()));
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }

    public record RectificationRequestDto(
        String fieldPath,
        String oldValue,
        String newValue,
        String reason
    ) {}

    public record ApproveRequestDto(String notes) {}

    public record RejectRequestDto(String reason) {}
}