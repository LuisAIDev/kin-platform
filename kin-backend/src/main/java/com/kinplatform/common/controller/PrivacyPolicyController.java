package com.kinplatform.common.controller;

import com.kinplatform.common.entity.PrivacyPolicyVersion;
import com.kinplatform.common.service.PrivacyPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PrivacyPolicyController {

    private final PrivacyPolicyService service;

    // PUBLIC ENDPOINTS
    @GetMapping("/api/v1/public/privacy-policy")
    public ResponseEntity<PrivacyPolicyVersion> getActivePolicy() {
        return service.getActivePolicy()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/public/privacy-policy/versions")
    public ResponseEntity<List<com.kinplatform.common.entity.PrivacyPolicyVersion>> getAllVersions() {
        return ResponseEntity.ok(service.listAllVersions());
    }

    @GetMapping("/api/v1/public/privacy-policy/{version}")
    public ResponseEntity<PrivacyPolicyVersion> getPolicyByVersion(@PathVariable String version) {
        return service.getPolicyByVersion(version)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ADMIN ENDPOINTS
    @PostMapping("/api/v1/admin/privacy-policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.kinplatform.common.entity.PrivacyPolicyVersion> publishNewVersion(
            Authentication auth,
            @RequestBody PublishRequestDto dto) {
        UUID adminId = UUID.fromString(auth.getName());
        com.kinplatform.common.entity.PrivacyPolicyVersion version = service.publishNewVersion(
            dto.version(), dto.title(), dto.contentMd(), dto.effectiveDate(), adminId);
        return ResponseEntity.status(201).body(version);
    }

    @PutMapping("/api/v1/admin/privacy-policy/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.kinplatform.common.entity.PrivacyPolicyVersion> activateVersion(
            @PathVariable UUID id,
            Authentication auth) {
        // For simplicity, this just re-activates an old version
        // In practice, you'd implement proper reactivation logic
        return ResponseEntity.notFound().build();
    }

    public record PublishRequestDto(
        String version,
        String title,
        String contentMd,
        java.time.LocalDate effectiveDate
    ) {}
}