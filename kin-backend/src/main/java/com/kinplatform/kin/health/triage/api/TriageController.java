package com.kinplatform.kin.health.triage.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del módulo de triaje digital (ADR-028).
 *
 * <ul>
 *   <li>{@code POST /api/v1/health/triage} — consulta de triaje por síntomas.</li>
 *   <li>{@code GET /api/v1/health/triage/symptoms} — catálogo de síntomas (formulario).</li>
 *   <li>{@code GET /api/v1/health/triage/history} — historial del paciente autenticado.</li>
 * </ul>
 *
 * <p>Protegido por JWT. El {@code userId} se resuelve SIEMPRE desde la
 * autenticación (nunca desde el body), garantizando el aislamiento por
 * paciente en el historial.</p>
 */
@RestController
@RequestMapping({
    "/health/triage",
    "/medical/triage"
})
public class TriageController {

    private static final Logger log = LoggerFactory.getLogger(TriageController.class);

    private final TriageService triageService;
    private final UserRepository userRepository;

    public TriageController(TriageService triageService, UserRepository userRepository) {
        this.triageService = triageService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<TriageResponse> triage(
            Authentication authentication, @Valid @RequestBody TriageRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== TRIAGE REQUEST === userId={}, symptoms={}", userId, request.symptoms());
        var result = triageService.analyze(userId, request.symptoms());
        var response =
                result.isEmpty() ? TriageResponse.empty(result.unrecognizedSymptoms()) : TriageResponse.success(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/symptoms")
    public ResponseEntity<List<SymptomResponse>> symptoms() {
        return ResponseEntity.ok(
                triageService.listSymptoms().stream().map(SymptomResponse::from).toList());
    }

    @GetMapping("/history")
    public ResponseEntity<List<TriageHistoryResponse>> history(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(triageService.historyExcludingHidden(userId).stream()
                .map(TriageHistoryResponse::from)
                .toList());
    }

    @DeleteMapping("/consultations/{consultationId}/hide")
    public ResponseEntity<Void> hideConsultation(
            @PathVariable UUID consultationId,
            Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        triageService.hideConsultation(consultationId, userId);
        log.info("Consulta {} ocultada por paciente {}", consultationId, userId);
        return ResponseEntity.noContent().build();
    }
}

