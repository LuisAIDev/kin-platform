package com.kinplatform.kin.health.physician.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint para el flujo "consentimiento en un clic" (ADR-040): el paciente
 * abre el enlace del correo de invitación, acepta el consentimiento de datos de
 * salud y queda vinculado al médico automáticamente.
 *
 * <p>Accesible a CUALQUIER usuario autenticado (matcher
 * {@code /health/patient/consent/**} en SecurityConfig): justo antes de aceptar
 * el consentimiento el usuario puede no tener aún capacidad de paciente
 * (ROLE_PATIENT), por lo que este endpoint no puede exigirla. El aislamiento es
 * estricto: el userId se resuelve SIEMPRE desde la autenticación, nunca del
 * body; solo se acepta una invitación dirigida a ese mismo usuario.</p>
 */
@RestController
@RequestMapping({
    "/health/patient/consent",
    "/medical/patient/consent"
})
public class PatientConsentController {

    private static final Logger log = LoggerFactory.getLogger(PatientConsentController.class);

    private final RelationshipService relationshipService;
    private final UserRepository userRepository;

    public PatientConsentController(RelationshipService relationshipService, UserRepository userRepository) {
        this.relationshipService = relationshipService;
        this.userRepository = userRepository;
    }

    /**
     * Acepta el consentimiento de datos de salud del usuario autenticado y
     * vincula la invitación PENDING_CONSENT con el médico indicado (→ ACTIVE).
     */
    @PostMapping("/accept")
    public ResponseEntity<PatientRelationshipController.RelationshipResponse> accept(
            Authentication authentication, @Valid @RequestBody ConsentAcceptRequest request) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        PhysicianPatientAssignment assignment =
                relationshipService.acceptWithConsent(patientId, request.physicianId());
        log.info("=== PATIENT CONSENT ACCEPT === patientId={}, physicianId={}", patientId, request.physicianId());
        return ResponseEntity.ok(PatientRelationshipController.RelationshipResponse.of(assignment));
    }

    public record ConsentAcceptRequest(
            @NotNull(message = "physicianId es obligatorio") UUID physicianId) {}
}
