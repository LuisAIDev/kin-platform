package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos del portal de médicos (ADR-031).
 *
 * <p>{@code POST /api/v1/admin/health/physician/assign} asigna un paciente a un
 * médico (MVP: asignación manual por administrador; futuras: por región o
 * especialidad). Protegido por rol ADMIN (SecurityConfig: {@code /admin/**}).</p>
 */
@RestController
@RequestMapping("/admin/health/physician")
public class PhysicianAdminController {

    private static final Logger log = LoggerFactory.getLogger(PhysicianAdminController.class);

    private final PhysicianService physicianService;

    public PhysicianAdminController(PhysicianService physicianService) {
        this.physicianService = physicianService;
    }

    @PostMapping("/assign")
    public ResponseEntity<AssignResponse> assign(@Valid @RequestBody AssignRequest request) {
        log.info("=== PHYSICIAN ASSIGN === physicianId={}, patientId={}", request.physicianId(), request.patientId());
        PhysicianPatientAssignment assignment =
                physicianService.assignPatient(request.physicianId(), request.patientId());
        return ResponseEntity.ok(AssignResponse.from(assignment));
    }

    public record AssignRequest(
            @NotNull(message = "physicianId es obligatorio") UUID physicianId,
            @NotNull(message = "patientId es obligatorio") UUID patientId) {}

    public record AssignResponse(UUID physicianId, UUID patientId) {
        static AssignResponse from(PhysicianPatientAssignment assignment) {
            return new AssignResponse(assignment.physicianId(), assignment.patientId());
        }
    }
}
