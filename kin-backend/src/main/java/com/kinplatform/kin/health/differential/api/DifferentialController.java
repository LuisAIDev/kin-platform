package com.kinplatform.kin.health.differential.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.user.UserRepository;
import jakarta.validation.Valid;
import java.util.HashSet;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del diagnóstico diferencial (ADR-029).
 *
 * <ul>
 *   <li>{@code GET /api/v1/health/differential?consultationId=...} — resultado
 *       de una consulta de triaje existente del paciente autenticado.</li>
 *   <li>{@code POST /api/v1/health/differential} — recibe síntomas directos y
 *       devuelve el diagnóstico diferencial (ejecuta triaje primero).</li>
 * </ul>
 *
 * <p>Protegido por JWT. El {@code userId} se resuelve SIEMPRE desde la
 * autenticación, garantizando el aislamiento por paciente.</p>
 */
@RestController
@RequestMapping({
    "/health/differential",
    "/medical/differential"
})
public class DifferentialController {

    private static final Logger log = LoggerFactory.getLogger(DifferentialController.class);

    private final DifferentialService differentialService;
    private final UserRepository userRepository;

    public DifferentialController(DifferentialService differentialService, UserRepository userRepository) {
        this.differentialService = differentialService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<DifferentialResponse> differentialByConsultation(
            Authentication authentication,
            @RequestParam UUID consultationId,
            @RequestParam(required = false) List<String> riskFactors) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== DIFFERENTIAL REQUEST === userId={}, consultationId={}", userId, consultationId);
        var result = differentialService.fromConsultation(
                userId, consultationId, new HashSet<>(riskFactors == null ? List.of() : riskFactors));
        var response = result.isEmpty()
                ? DifferentialResponse.empty(result.unrecognizedSymptoms())
                : DifferentialResponse.success(result);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DifferentialResponse> differential(
            Authentication authentication, @Valid @RequestBody DifferentialRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== DIFFERENTIAL REQUEST === userId={}, symptoms={}", userId, request.symptoms());
        var result = differentialService.fromSymptoms(request.symptoms(), new HashSet<>(request.riskFactors()));
        var response = result.isEmpty()
                ? DifferentialResponse.empty(result.unrecognizedSymptoms())
                : DifferentialResponse.success(result);
        return ResponseEntity.ok(response);
    }
}

