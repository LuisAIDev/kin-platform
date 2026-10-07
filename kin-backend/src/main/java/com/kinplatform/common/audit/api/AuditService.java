package com.kinplatform.common.audit.api;

import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import com.kinplatform.common.audit.config.AuditProperties;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.common.audit.event.AuditLogEvent;
import com.kinplatform.common.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servicio de auditoría de accesos a datos de salud (ADR-035).
 *
 * <p>Publica {@link AuditLogEvent} de forma transaccional (outbox, fallback al
 * bus en memoria) para que el {@code AuditLogEventListener} persista el log de
 * forma asíncrona, sin bloquear la operación principal. La auditoría nunca rompe
 * el flujo: si el módulo está deshabilitado o falla, se registra y continúa.</p>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final DomainEventBus eventBus;
    private final OutboxEventPublisher outboxEventPublisher;
    private final UserRepository userRepository;
    private final AuditProperties properties;

    public AuditService(
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher,
            UserRepository userRepository,
            AuditProperties properties) {
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    /**
     * Registra un acceso a datos de salud del usuario {@code userId}.
     * IP y user-agent se capturan de la petición actual si están disponibles.
     */
    public void logAccess(
            UUID userId,
            AuditAction action,
            AuditResourceType resourceType,
            UUID resourceId,
            UUID patientId,
            Map<String, Object> details) {
        if (!properties.isEnabled() || userId == null || action == null || resourceType == null) {
            return;
        }
        try {
            AuditLogEvent event = new AuditLogEvent(
                    userId, action, resourceType, resourceId, patientId,
                    currentIp(), currentUserAgent(), details);
            if (outboxEventPublisher != null) {
                outboxEventPublisher.publish(event);
            } else if (eventBus != null) {
                eventBus.publish(event);
            }
        } catch (Exception e) {
            // La auditoría nunca debe romper el flujo principal.
            log.warn("AuditService: no se pudo registrar el acceso ({}): {}", action, e.getMessage());
        }
    }

    /** Registra un acceso resolviendo el userId desde el {@link SecurityContext}. */
    public void logAccessFromPrincipal(
            AuditAction action,
            AuditResourceType resourceType,
            UUID resourceId,
            UUID patientId,
            Map<String, Object> details) {
        UUID userId = currentUserId();
        if (userId == null) {
            return;
        }
        logAccess(userId, action, resourceType, resourceId, patientId, details);
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(u -> u.getId()).orElse(null);
    }

    private static String currentIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "";
        }
        String ua = request.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}




