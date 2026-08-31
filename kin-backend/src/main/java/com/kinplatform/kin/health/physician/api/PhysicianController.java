package com.kinplatform.kin.health.physician.api;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.triage.api.TriageHistoryResponse;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del portal de médicos (ADR-031 + ciclo de vida V30).
 *
 * <ul>
 *   <li>{@code GET /api/v1/health/physician/patients?status=ACTIVE|PENDING|ALL} — pacientes (cartera o invitaciones).</li>
 *   <li>{@code GET /api/v1/health/physician/patients/{patientId}/summary} — resumen clínico.</li>
 *   <li>{@code GET /api/v1/health/physician/patients/{patientId}/history} — historial.</li>
 *   <li>{@code GET /api/v1/health/physician/alerts} — alertas activas.</li>
 *   <li>{@code POST /api/v1/health/physician/alerts/{alertId}/acknowledge} — marcar atendida.</li>
 *   <li>{@code POST /api/v1/health/physician/patients/invite} — invitar a un paciente (por email).</li>
 * </ul>
 *
 * <p>Protegido por JWT con rol {@code PHYSICIAN}. El médico solo ve pacientes
 * con relación activa (aislamiento estricto; acceso a un paciente no asignado
 * devuelve 404).</p>
 */
@RestController
@RequestMapping("/health/physician")
public class PhysicianController {

    private static final Logger log = LoggerFactory.getLogger(PhysicianController.class);

    private final PhysicianService physicianService;
    private final RelationshipService relationshipService;
    private final UserRepository userRepository;

    public PhysicianController(
            PhysicianService physicianService, RelationshipService relationshipService, UserRepository userRepository) {
        this.physicianService = physicianService;
        this.relationshipService = relationshipService;
        this.userRepository = userRepository;
    }

    @GetMapping("/patients")
    public ResponseEntity<PageResponse<PatientSummaryResponse>> patients(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {
        UUID physicianId = AuthenticatedUsers.require(userRepository, authentication).getId();
        RelationshipStatus resolved = resolveStatus(status);
        var page = resolved == null
                ? physicianService.listPatients(physicianId, pageable)
                : physicianService.listPatients(physicianId, resolved, pageable);
        log.info("=== PHYSICIAN PATIENTS === physicianId={}, status={}", physicianId, status);
        return ResponseEntity.ok(PageResponse.from(page.map(PatientSummaryResponse::from)));
    }

    @PostMapping("/patients/invite")
    public ResponseEntity<InvitationResponse> invitePatient(
            Authentication authentication, @Valid @RequestBody InviteRequest request) {
        UUID physicianId = AuthenticatedUsers.require(userRepository, authentication).getId();
        PhysicianPatientAssignment invitation =
                relationshipService.invitePatient(physicianId, request.patientEmail(), request.message());
        String patientEmail = userRepository
                .findById(invitation.patientId())
                .map(User::getEmail)
                .orElse(request.patientEmail());
        log.info("=== PHYSICIAN INVITE === physicianId={}, patient={}", physicianId, invitation.patientId());
        return ResponseEntity.ok(InvitationResponse.from(invitation, patientEmail));
    }

    @GetMapping("/patients/{patientId}/summary")
    public ResponseEntity<PatientSummaryResponse> patientSummary(
            Authentication authentication, @PathVariable UUID patientId) {
        UUID physicianId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(PatientSummaryResponse.from(physicianService.patientSummary(physicianId, patientId)));
    }

    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<List<TriageHistoryResponse>> patientHistory(
            Authentication authentication, @PathVariable UUID patientId) {
        UUID physicianId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(physicianService.patientHistory(physicianId, patientId).stream()
                .map(TriageHistoryResponse::from)
                .toList());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<ClinicalAlertResponse>> alerts(Authentication authentication) {
        UUID physicianId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(physicianService.activeAlerts(physicianId).stream()
                .map(ClinicalAlertResponse::from)
                .toList());
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<ClinicalAlertResponse> acknowledge(
            Authentication authentication, @PathVariable UUID alertId) {
        UUID physicianId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== PHYSICIAN ALERT ACKNOWLEDGE === physicianId={}, alertId={}", physicianId, alertId);
        return ResponseEntity.ok(ClinicalAlertResponse.from(physicianService.acknowledgeAlert(physicianId, alertId)));
    }

    private static RelationshipStatus resolveStatus(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        try {
            return RelationshipStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record InviteRequest(
            @NotBlank(message = "patientEmail es obligatorio")
                    @Email(message = "El correo no tiene un formato válido")
                    String patientEmail,
            String message) {}

    public record InvitationResponse(
            UUID physicianId,
            UUID patientId,
            RelationshipStatus status,
            OffsetDateTime invitedAt,
            String patientEmail) {
        static InvitationResponse from(PhysicianPatientAssignment assignment, String patientEmail) {
            return new InvitationResponse(
                    assignment.physicianId(),
                    assignment.patientId(),
                    assignment.status(),
                    assignment.invitedAt(),
                    patientEmail);
        }
    }
}
