package com.kinplatform.kin.health.notifications;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.user.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint unificado de contadores de notificaciones dentro de la plataforma.
 *
 * <p>{@code GET /api/v1/health/notifications/counts} devuelve los contadores
 * relevantes según el rol del usuario autenticado (PATIENT o PHYSICIAN).
 * Alimenta los badges del sidebar sin necesidad de consultar cada módulo.</p>
 */
@RestController
@RequestMapping({"/health/notifications", "/medical/notifications"})
public class NotificationCountsController {

    private static final Logger log = LoggerFactory.getLogger(NotificationCountsController.class);

    private final NotificationCountsService notificationCountsService;
    private final UserRepository userRepository;

    public NotificationCountsController(
            NotificationCountsService notificationCountsService, UserRepository userRepository) {
        this.notificationCountsService = notificationCountsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/counts")
    public ResponseEntity<NotificationCounts> counts(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        NotificationCounts counts = notificationCountsService.countsFor(userId);
        log.debug("=== NOTIFICATION COUNTS === userId={}, total={}", userId, counts.total());
        return ResponseEntity.ok(counts);
    }
}

