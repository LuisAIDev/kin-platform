package com.kinplatform.kin.health.physician.api;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.triage.api.TriageHistoryResponse;
import com.kinplatform.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del portal de médicos (ADR-031).
 *
 * <ul>
 *   <li>{@code GET /api/v1/health/physician/patients} — pacientes asignados.</li>
 *   <li>{@code GET /api/v1/health/physician/patients/{patientId}/summary} — resumen clínico.</li>
 *   <li>{@code GET /api/v1/health/physician/patients/{patientId}/history} — historial.</li>
 *   <li>{@code GET /api/v1/health/physician/alerts} — alertas activas.</li>
 *   <li>{@code POST /api/v1/health/physician/alerts/{alertId}/acknowledge} — marcar atendida.</li>
 * </ul>
 *
 * <p>Protegido por JWT con rol {@code PHYSICIAN}. El médico solo ve pacientes
 * asignados a él (aislamiento estricto; acceso a un paciente no asignado
 * devuelve 404).</p>
 */
@RestController
@RequestMapping("/health/physician")
public class PhysicianController {

    private static final Logger log = LoggerFactory.getLogger(PhysicianController.class);

    private final PhysicianService physicianService;
    private final UserRepository userRepository;

    public PhysicianController(PhysicianService physicianService, UserRepository userRepository) {
        this.physicianService = physicianService;
        this.userRepository = userRepository;
    }

    @GetMapping("/patients")
    public ResponseEntity<PageResponse<PatientSummaryResponse>> patients(
            Authentication authentication, @PageableDefault(size = 10) Pageable pageable) {
        UUID physicianId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== PHYSICIAN PATIENTS === physicianId={}", physicianId);
        var page = physicianService.listPatients(physicianId, pageable);
        return ResponseEntity.ok(PageResponse.from(page.map(PatientSummaryResponse::from)));
    }

    @GetMapping("/patients/{patientId}/summary")
    public ResponseEntity<PatientSummaryResponse> patientSummary(
            Authentication authentication, @PathVariable UUID patientId) {
        UUID physicianId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(PatientSummaryResponse.from(physicianService.patientSummary(physicianId, patientId)));
    }

    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<List<TriageHistoryResponse>> patientHistory(
            Authentication authentication, @PathVariable UUID patientId) {
        UUID physicianId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(physicianService.patientHistory(physicianId, patientId).stream()
                .map(TriageHistoryResponse::from)
                .toList());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<ClinicalAlertResponse>> alerts(Authentication authentication) {
        UUID physicianId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(physicianService.activeAlerts(physicianId).stream()
                .map(ClinicalAlertResponse::from)
                .toList());
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<ClinicalAlertResponse> acknowledge(
            Authentication authentication, @PathVariable UUID alertId) {
        UUID physicianId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== PHYSICIAN ALERT ACKNOWLEDGE === physicianId={}, alertId={}", physicianId, alertId);
        return ResponseEntity.ok(ClinicalAlertResponse.from(physicianService.acknowledgeAlert(physicianId, alertId)));
    }
}
