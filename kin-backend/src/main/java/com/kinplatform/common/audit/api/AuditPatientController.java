package com.kinplatform.common.audit.api;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.audit.port.AuditLogRepository;
import com.kinplatform.common.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de transparencia para pacientes (ADR-035).
 *
 * <p>{@code GET /api/v1/health/audit/my-logs} devuelve quién ha accedido a los
 * datos de salud del paciente autenticado (aislamiento por userId del JWT).
 * Protegido por rol PATIENT/ADMIN (SecurityConfig: {@code /health/audit/**}).</p>
 */
@RestController
@RequestMapping({
    "/health/audit",
    "/medical/audit"
})
public class AuditPatientController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditPatientController(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-logs")
    public ResponseEntity<PageResponse<AuditLogResponse>> myLogs(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        var page = auditLogRepository
                .findByPatientIdAndDateRange(patientId, startDate, endDate, pageable)
                .map(AuditLogResponse::from);
        return ResponseEntity.ok(PageResponse.from(page));
    }
}



