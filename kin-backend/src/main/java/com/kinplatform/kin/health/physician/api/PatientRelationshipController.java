package com.kinplatform.kin.health.physician.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del paciente para gestionar sus relaciones con médicos
 * (ciclo de vida V30).
 *
 * <ul>
 *   <li>{@code GET /api/v1/health/patient/relationships/pending} — invitaciones pendientes
 *       ({@code PENDING} y {@code PENDING_CONSENT}).</li>
 *   <li>{@code POST /api/v1/health/patient/relationships/accept} — aceptar una invitación
 *       ({@code PENDING}; las {@code PENDING_CONSENT} se aceptan por
 *       {@code /health/patient/consent/accept}).</li>
 *   <li>{@code POST /api/v1/health/patient/relationships/reject} — rechazar una invitación.</li>
 * </ul>
 *
 * <p>Aislamiento estricto: el paciente solo opera sobre invitaciones dirigidas
 * a él (el userId se resuelve siempre desde la autenticación, nunca del body).
 * Los matchers de {@code SecurityConfig} exigen solo autenticación para
 * {@code /health/patient/relationships/**} porque antes de aceptar el
 * consentimiento de salud (estado {@code PENDING_CONSENT}) el usuario aún no
 * tiene {@code ROLE_PATIENT}, pero debe poder ver/actuar sobre su invitación.</p>
 */
@RestController
@RequestMapping("/health/patient/relationships")
public class PatientRelationshipController {

    private static final Logger log = LoggerFactory.getLogger(PatientRelationshipController.class);

    private final RelationshipService relationshipService;
    private final UserRepository userRepository;

    public PatientRelationshipController(RelationshipService relationshipService, UserRepository userRepository) {
        this.relationshipService = relationshipService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingInvitationResponse>> pending(Authentication authentication) {
        UUID patientId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        List<PendingInvitationResponse> invitations =
                relationshipService.pendingInvitationsForPatient(patientId).stream()
                        .map(this::toPendingResponse)
                        .toList();
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/accept")
    public ResponseEntity<RelationshipResponse> accept(
            Authentication authentication, @Valid @RequestBody PhysicianRequest request) {
        UUID patientId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        PhysicianPatientAssignment accepted = relationshipService.acceptInvitation(patientId, request.physicianId());
        log.info("=== PATIENT RELATIONSHIP ACCEPT === patientId={}, physicianId={}", patientId, request.physicianId());
        return ResponseEntity.ok(RelationshipResponse.of(accepted));
    }

    @PostMapping("/reject")
    public ResponseEntity<RelationshipResponse> reject(
            Authentication authentication, @Valid @RequestBody PhysicianRequest request) {
        UUID patientId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        PhysicianPatientAssignment ended = relationshipService.rejectInvitation(patientId, request.physicianId());
        log.info("=== PATIENT RELATIONSHIP REJECT === patientId={}, physicianId={}", patientId, request.physicianId());
        return ResponseEntity.ok(RelationshipResponse.of(ended));
    }

    private PendingInvitationResponse toPendingResponse(PhysicianPatientAssignment assignment) {
        User physician = userRepository.findById(assignment.invitedBy()).orElse(null);
        String physicianName = physician == null ? "Médico" : physician.getFullName();
        String specialty = physician == null ? null : physician.getSpecialty();
        return new PendingInvitationResponse(
                assignment.physicianId(),
                physicianName,
                specialty,
                assignment.invitedAt(),
                assignment.status(),
                assignment.isPendingConsent());
    }

    public record PhysicianRequest(@NotNull(message = "physicianId es obligatorio") UUID physicianId) {}

    public record PendingInvitationResponse(
            UUID physicianId,
            String physicianName,
            String specialty,
            OffsetDateTime invitedAt,
            RelationshipStatus status,
            boolean consentRequired) {}

    public record RelationshipResponse(
            UUID physicianId,
            UUID patientId,
            RelationshipStatus status,
            OffsetDateTime acceptedAt,
            OffsetDateTime endedAt,
            String endedReason) {
        static RelationshipResponse of(PhysicianPatientAssignment assignment) {
            return new RelationshipResponse(
                    assignment.physicianId(),
                    assignment.patientId(),
                    assignment.status(),
                    assignment.acceptedAt(),
                    assignment.endedAt(),
                    assignment.endedReason());
        }
    }
}
