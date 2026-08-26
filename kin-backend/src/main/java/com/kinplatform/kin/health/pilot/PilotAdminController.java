package com.kinplatform.kin.health.pilot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint administrativo de onboarding del grupo piloto (fase piloto).
 *
 * <p>{@code POST /api/v1/admin/health/pilot/setup} crea de forma idempotente
 * los usuarios del piloto (pacientes/médicos), los marca verificados y activos,
 * y aplica las asignaciones paciente → médico. Protegido por rol ADMIN
 * (SecurityConfig: {@code /admin/**}). Los correos y la contraseña se envían en
 * el body; la contraseña común se entrega a los participantes de forma segura
 * fuera de banda.</p>
 */
@RestController
@RequestMapping("/admin/health/pilot")
public class PilotAdminController {

    private static final Logger log = LoggerFactory.getLogger(PilotAdminController.class);

    private final PilotOnboardingService onboardingService;
    private final PilotMetricsService metricsService;

    public PilotAdminController(PilotOnboardingService onboardingService, PilotMetricsService metricsService) {
        this.onboardingService = onboardingService;
        this.metricsService = metricsService;
    }

    @PostMapping("/setup")
    public ResponseEntity<PilotSetupResponse> setup(@Valid @RequestBody PilotSetupRequest request) {
        log.info(
                "=== PILOT SETUP === pacientes={}, médicos={}, asignaciones={}",
                request.patients().size(),
                request.physicians().size(),
                request.assignments() == null ? 0 : request.assignments().size());
        var result = onboardingService.setup(
                request.patients(),
                request.physicians(),
                request.password(),
                request.assignments().stream()
                        .map(a -> new PilotOnboardingService.Assignment(a.patientEmail(), a.physicianEmail()))
                        .toList());
        return ResponseEntity.ok(new PilotSetupResponse(result.usersCreated(), result.assignmentsCreated()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<PilotMetricsService.PilotMetrics> metrics() {
        log.info("=== PILOT METRICS (admin) ===");
        return ResponseEntity.ok(metricsService.report());
    }

    public record PilotSetupRequest(
            @NotEmpty(message = "Se requiere al menos un paciente") List<@NotBlank @Email String> patients,
            @NotEmpty(message = "Se requiere al menos un médico") List<@NotBlank @Email String> physicians,
            @NotBlank(message = "La contraseña es obligatoria")
                    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
                    String password,
            List<AssignmentRequest> assignments) {
        public PilotSetupRequest {
            assignments = assignments == null ? List.of() : List.copyOf(assignments);
        }
    }

    public record AssignmentRequest(@NotBlank @Email String patientEmail, @NotBlank @Email String physicianEmail) {}

    public record PilotSetupResponse(int usersCreated, int assignmentsCreated) {}
}
