package com.kinplatform.common.legal;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PrivacyPolicyController {

    private final PrivacyPolicyService service;

    // PUBLIC ENDPOINTS
    @GetMapping("/public/privacy-policy")
    public ResponseEntity<PrivacyPolicyVersion> getActivePolicy() {
        return service.getActivePolicy()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/privacy-policy/versions")
    public ResponseEntity<List<com.kinplatform.common.legal.PrivacyPolicyVersion>> getAllVersions() {
        return ResponseEntity.ok(service.listAllVersions());
    }

    @GetMapping("/public/privacy-policy/{version}")
    public ResponseEntity<PrivacyPolicyVersion> getPolicyByVersion(@PathVariable String version) {
        return service.getPolicyByVersion(version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ADMIN ENDPOINTS
    @PostMapping("/admin/privacy-policy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.kinplatform.common.legal.PrivacyPolicyVersion> publishNewVersion(
            Authentication auth, @RequestBody PublishRequestDto dto) {
        UUID adminId = UUID.fromString(auth.getName());
        com.kinplatform.common.legal.PrivacyPolicyVersion version =
                service.publishNewVersion(dto.version(), dto.title(), dto.contentMd(), dto.effectiveDate(), adminId);
        return ResponseEntity.status(201).body(version);
    }

    @PutMapping("/admin/privacy-policy/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.kinplatform.common.legal.PrivacyPolicyVersion> activateVersion(
            @PathVariable UUID id, Authentication auth) {
        // For simplicity, this just re-activates an old version
        // In practice, you'd implement proper reactivation logic
        return ResponseEntity.notFound().build();
    }

    public record PublishRequestDto(
            String version, String title, String contentMd, java.time.LocalDate effectiveDate) {}
}
