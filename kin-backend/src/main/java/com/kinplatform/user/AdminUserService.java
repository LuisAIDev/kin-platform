package com.kinplatform.user;

import com.kinplatform.auth.password.PasswordResetService;
import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de usuarios (vertical Salud): listado y verificación de
 * solicitudes de capacidad profesional (PHYSICIAN), INDEPENDIENTEMENTE de la
 * persona/rol del solicitante ({@code users.role}).
 *
 * <p>Alternativa B (KIN Salud 2.0): la capacidad profesional está desacoplada
 * de {@code users.role}. Un usuario existente (FREE/PREMIUM/PATIENT/…) puede
 * solicitar ser profesional; el ADMIN decide sobre la solicitud sin modificar
 * el rol, el plan ni la suscripción. Cada decisión queda auditada
 * ({@link AuditService}) con el ADMIN como actor y el usuario como recurso.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PendingPhysicianResponse> pendingPhysicians() {
        return userRepository
                .findByPhysicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .stream()
                .map(u -> PendingPhysicianResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .role(u.getRole() == null ? null : u.getRole().name())
                        .licenseNumber(u.getLicenseNumber())
                        .specialty(u.getSpecialty())
                        .country(u.getCountry())
                        .phone(u.getPhone())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Aprueba la solicitud profesional pendiente del usuario indicado. El rol,
     * plan y suscripción del usuario NO se modifican: solo pasa su
     * {@code physician_verification_status} a {@code APPROVED} (la capacidad
     * PHYSICIAN se deriva por request vía {@code PhysicianAccess}).
     */
    @Transactional
    public void setVerificationStatus(UUID userId, PhysicianVerificationStatus status) {
        setVerificationStatus(userId, status, null);
    }

    /**
     * Decide sobre la solicitud profesional pendiente del usuario indicado,
     * permitiendo a cualquier rol (no solo PHYSICIAN) ser revisado por el ADMIN.
     *
     * <p>Solo se permite decidir sobre solicitudes en {@code PENDING}. La
     * decisión queda registrada en {@code audit_logs} (actor = ADMIN autenticado,
     * recurso = usuario afectado, motivo en {@code details}).</p>
     *
     * @param userId usuario cuya solicitud se decide.
     * @param status {@code APPROVED} o {@code REJECTED}.
     * @param reason motivo del rechazo (opcional; se guarda en auditoría).
     */
    @Transactional
    public void setVerificationStatus(UUID userId, PhysicianVerificationStatus status, String reason) {
        if (status == null || status == PhysicianVerificationStatus.PENDING) {
            throw new IllegalArgumentException("El estado de decisión debe ser APPROVED o REJECTED");
        }
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.getPhysicianVerificationStatus() != PhysicianVerificationStatus.PENDING) {
            throw new IllegalArgumentException("El usuario no tiene una solicitud profesional pendiente de revisión");
        }

        PhysicianVerificationStatus previous = user.getPhysicianVerificationStatus();
        user.setPhysicianVerificationStatus(status);
        userRepository.save(user);

        logDecision(user.getId(), status, previous, reason);
    }

    /**
     * Registra la decisión ADMIN en la auditoría (actor = principal autenticado,
     * recurso = usuario afectado). La auditoría nunca rompe el flujo.
     */
    private void logDecision(
            UUID targetUserId,
            PhysicianVerificationStatus newStatus,
            PhysicianVerificationStatus previousStatus,
            String reason) {
        AuditAction action = newStatus == PhysicianVerificationStatus.APPROVED
                ? AuditAction.PHYSICIAN_APPLICATION_APPROVED
                : AuditAction.PHYSICIAN_APPLICATION_REJECTED;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousStatus", previousStatus == null ? null : previousStatus.name());
        details.put("newStatus", newStatus == null ? null : newStatus.name());
        details.put("reviewedAt", OffsetDateTime.now().toString());
        if (reason != null && !reason.isBlank()) {
            details.put("reason", reason.trim());
        }

        auditService.logAccessFromPrincipal(action, AuditResourceType.USER, targetUserId, null, details);
    }

    /**
     * Marca el email de un usuario como verificado manualmente (último recurso
     * operativo cuando los correos no llegan). Requiere rol ADMIN.
     */
    @Transactional
    public void verifyEmail(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Genera un enlace de restablecimiento de contraseña sin enviar correo
     * (último recurso operativo). Requiere rol ADMIN.
     *
     * @return URL completa del formulario de reset con el token.
     */
    @Transactional
    public String generateResetLink(UUID userId) {
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return passwordResetService.generateResetLink(userId);
    }
}

