package com.kinplatform.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints ADMIN para la verificación de solicitudes de capacidad profesional
 * (vertical Salud). Acceso restringido a rol ADMIN vía {@code SecurityConfig}.
 *
 * <p>Las solicitudes pueden provenir de cualquier persona/rol (FREE/PREMIUM/
 * PATIENT/PHYSICIAN). Aprobar/rechazar NO modifica {@code users.role} ni el plan
 * del solicitante: solo cambia {@code physician_verification_status} y queda
 * auditado.</p>
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
    public ResponseEntity<Void> rejectPhysician(@PathVariable UUID userId, @RequestBody(required = false) RejectRequest body) {
        String reason = body == null ? null : body.reason();
        adminUserService.setVerificationStatus(userId, PhysicianVerificationStatus.REJECTED, reason);
        return ResponseEntity.ok().build();
    }

    /** Cuerpo opcional del rechazo con el motivo (se guarda en audit_logs.details). */
    public record RejectRequest(String reason) {}

    /**
     * Marca el email de un usuario como verificado manualmente (último recurso
     * operativo si los correos de verificación no llegan).
     */
    @PostMapping("/{userId}/verify")
    public ResponseEntity<Void> verifyEmail(@PathVariable UUID userId) {
        adminUserService.verifyEmail(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Genera un enlace de restablecimiento de contraseña sin enviar correo
     * (último recurso si los correos de recuperación no llegan).
     */
    @PostMapping("/{userId}/reset-password-link")
    public ResponseEntity<Map<String, String>> resetPasswordLink(@PathVariable UUID userId) {
        String link = adminUserService.generateResetLink(userId);
        return ResponseEntity.ok(Map.of("resetUrl", link));
    }
}
