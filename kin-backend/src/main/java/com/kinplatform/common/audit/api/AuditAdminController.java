package com.kinplatform.common.audit.api;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.port.AuditLogRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de auditoría para administradores (ADR-035).
 *
 * <p>{@code GET /api/v1/admin/health/audit/logs} con filtros por usuario,
 * paciente, acción y rango de fechas. Protegido por rol ADMIN
 * (SecurityConfig: {@code /admin/**}).</p>
 */
@RestController
@RequestMapping({
    "/admin/health/audit",
    "/medical/admin/audit"
})
public class AuditAdminController {

    private static final Logger log = LoggerFactory.getLogger(AuditAdminController.class);

    private final AuditLogRepository auditLogRepository;

    public AuditAdminController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/logs")
    public ResponseEntity<PageResponse<AuditLogResponse>> logs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = auditLogRepository
                .search(userId, patientId, action, startDate, endDate, pageable)
                .map(AuditLogResponse::from);
        log.info("=== AUDIT LOGS === userId={}, patientId={}, action={}", userId, patientId, action);
        return ResponseEntity.ok(PageResponse.from(page));
    }
}


