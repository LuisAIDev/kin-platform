package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.PhysicianVerificationStatus;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de solicitudes de capacidad profesional (médico).
 *
 * <p>Permite a usuarios autenticados con email verificado solicitar la capacidad
 * profesional (PHYSICIAN). El estado de la solicitud se guarda en
 * {@code users.physician_verification_status} y se audita cada acción.</p>
 */
@Service
public class PhysicianApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PhysicianApplicationService.class);

    private final UserRepository userRepository;
    private final AuditService auditService;

    public PhysicianApplicationService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Crea una nueva solicitud de capacidad profesional (médico).
     *
     * @param userId ID del usuario autenticado
     * @param request datos profesionales de la solicitud
     * @return estado actual de la solicitud (PENDING)
     */
    @Transactional
    public PhysicianVerificationStatus requestApplication(UUID userId, PhysicianApplicationRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar email verificado
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("El correo electrónico debe estar verificado para solicitar capacidad profesional");
        }

        // Validar que no existe solicitud previa en PENDING o APPROVED
        PhysicianVerificationStatus currentStatus = user.getPhysicianVerificationStatus();
        if (currentStatus == PhysicianVerificationStatus.PENDING) {
            throw new IllegalStateException("Ya existe una solicitud pendiente de revisión");
        }
        if (currentStatus == PhysicianVerificationStatus.APPROVED) {
            throw new IllegalStateException("La cuenta ya está aprobada como médico");
        }
        if (currentStatus == PhysicianVerificationStatus.REJECTED) {
            // Permitir re-solicitar después de un rechazo
            log.info("Usuario {} re-solicita capacidad profesional tras rechazo", userId);
        }

        // Actualizar datos profesionales del usuario
        user.setLicenseNumber(request.getLicenseNumber());
        user.setSpecialty(request.getSpecialty());
        user.setCountry(request.getCountry());
        user.setPhone(request.getPhone());
        user.setHealthDataConsent(Boolean.TRUE.equals(request.getHealthDataConsent()));
        user.setPhysicianVerificationStatus(PhysicianVerificationStatus.PENDING);
        userRepository.save(user);

        // Auditoría: solicitud enviada
        Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("licenseNumber", request.getLicenseNumber());
        details.put("specialty", request.getSpecialty());
        details.put("country", request.getCountry());
        details.put("phone", request.getPhone());
        details.put("requestedAt", java.time.OffsetDateTime.now().toString());

        auditService.logAccessFromPrincipal(
                AuditAction.PHYSICIAN_APPLICATION_REQUESTED,
                com.kinplatform.kin.health.audit.domain.AuditResourceType.USER,
                user.getId(),
                null,
                new java.util.LinkedHashMap<>(java.util.Map.of(
                        "licenseNumber", request.getLicenseNumber(),
                        "specialty", request.getSpecialty(),
                        "country", request.getCountry(),
                        "phone", request.getPhone(),
                        "requestedAt", java.time.OffsetDateTime.now().toString()
                ))
        );

        log.info("Usuario {} ha solicitado capacidad profesional (PENDING)", userId);
        return PhysicianVerificationStatus.PENDING;
    }

    /**
     * Consulta el estado de la solicitud de capacidad profesional del usuario autenticado.
     *
     * @param userId ID del usuario autenticado
     * @return estado actual de la solicitud
     */
    @Transactional(readOnly = true)
    public ApplicationStatusResponse getApplicationStatus(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        PhysicianVerificationStatus status = user.getPhysicianVerificationStatus();
        String statusStr;
        if (status == null) {
            statusStr = "NOT_FOUND";
        } else {
            statusStr = status.name();
        }

        return new ApplicationStatusResponse(
                statusStr,
                user.getRole() != null ? user.getRole().name() : null,
                user.getLicenseNumber(),
                user.getSpecialty(),
                user.getCountry(),
                user.getPhone(),
                user.getPhysicianVerificationStatus()
        );
    }

    public record ApplicationStatusResponse(
            String status,
            String role,
            String licenseNumber,
            String specialty,
            String country,
            String phone,
            PhysicianVerificationStatus physicianVerificationStatus
    ) {}
}