package com.kinplatform.user;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints ADMIN para la verificación de identidad de médicos auto-registrados
 * (vertical Salud). Acceso restringido a rol ADMIN vía {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/physicians/pending")
    public List<PendingPhysicianResponse> pendingPhysicians() {
        return adminUserService.pendingPhysicians();
    }

    @PostMapping("/physicians/{userId}/approve")
    public ResponseEntity<Void> approvePhysician(@PathVariable UUID userId) {
        adminUserService.setVerificationStatus(userId, PhysicianVerificationStatus.APPROVED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/physicians/{userId}/reject")
    public ResponseEntity<Void> rejectPhysician(@PathVariable UUID userId) {
        adminUserService.setVerificationStatus(userId, PhysicianVerificationStatus.REJECTED);
        return ResponseEntity.ok().build();
    }
}
