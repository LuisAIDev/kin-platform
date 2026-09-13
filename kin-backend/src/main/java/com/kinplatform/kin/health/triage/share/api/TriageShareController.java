package com.kinplatform.kin.health.triage.share.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.triage.share.application.TriageShareService;
import com.kinplatform.kin.health.triage.share.domain.TriageShareLink;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de compartición temporal de un informe de triaje con un médico
 * externo (fuera de KIN).
 *
 * <ul>
 *   <li>{@code POST /health/triage/{triageId}/share} — paciente autenticado
 *       genera/reutiliza el enlace (requiere plan Personal+).</li>
 *   <li>{@code GET /health/triage/share/{token}} — PÚBLICO, sin autenticación.
 *       Devuelve el contenido mínimo del triaje si el token es válido.</li>
 *   <li>{@code DELETE /health/triage/{triageId}/share} — paciente autenticado
 *       revoca el enlace antes de que expire.</li>
 * </ul>
 */
@RestController
@RequestMapping({"/health/triage", "/medical/triage"})
public class TriageShareController {

    private final TriageShareService shareService;
    private final UserRepository userRepository;

    @Value("${medical.frontend.base-url:https://www.kin-platform-medical.com}")
    private String medicalFrontendBaseUrl;

    public TriageShareController(TriageShareService shareService, UserRepository userRepository) {
        this.shareService = shareService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{triageId}/share")
    public ResponseEntity<TriageShareResponse> createShare(
            Authentication auth, @PathVariable UUID triageId) {
        User user = requireUser(auth);
        TriageShareLink link = shareService.createShare(user.getId(), triageId);
        return ResponseEntity.ok(toResponse(link));
    }

    @DeleteMapping("/{triageId}/share")
    public ResponseEntity<Void> revokeShare(
            Authentication auth, @PathVariable UUID triageId) {
        User user = requireUser(auth);
        shareService.revokeShare(user.getId(), triageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<SharedTriageContent> getSharedContent(@PathVariable String token) {
        SharedTriageContent content = shareService.resolvePublicContent(token);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(content);
    }

    private User requireUser(Authentication auth) {
        return AuthenticatedUsers.require(userRepository, auth);
    }

    private TriageShareResponse toResponse(TriageShareLink link) {
        String base = medicalFrontendBaseUrl == null || medicalFrontendBaseUrl.isBlank()
                ? "https://www.kin-platform-medical.com"
                : medicalFrontendBaseUrl;
        String url = base + "/share/" + link.token();
        return new TriageShareResponse(link.token(), url, link.expiresAt());
    }
}
