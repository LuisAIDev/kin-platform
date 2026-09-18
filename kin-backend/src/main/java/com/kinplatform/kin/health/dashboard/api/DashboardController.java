package com.kinplatform.kin.health.dashboard.api;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.triage.api.TriageHistoryResponse;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST del dashboard de salud del paciente (ADR-030).
 *
 * <ul>
 *   <li>{@code GET /api/v1/health/dashboard/summary} — resumen de salud.</li>
 *   <li>{@code GET /api/v1/health/dashboard/history} — historial paginado.</li>
 *   <li>{@code GET /api/v1/health/dashboard/history/{consultationId}} — detalle.</li>
 *   <li>{@code GET/PUT /api/v1/health/dashboard/profile} — perfil del paciente.</li>
 *   <li>{@code GET /api/v1/health/dashboard/care-plan} — plan de cuidado.</li>
 *   <li>{@code POST/GET /api/v1/health/dashboard/reminders} — recordatorios.</li>
 * </ul>
 *
 * <p>Protegido por JWT. El {@code userId} se resuelve SIEMPRE desde la
 * autenticación, garantizando el aislamiento por paciente.</p>
 */
@RestController
@RequestMapping({
    "/health/dashboard",
    "/medical/dashboard"
})
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    public DashboardController(DashboardService dashboardService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<HealthSummaryResponse> summary(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== DASHBOARD SUMMARY === userId={}", userId);
        return ResponseEntity.ok(HealthSummaryResponse.from(dashboardService.summary(userId)));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<TriageHistoryResponse>> history(
            Authentication authentication, @PageableDefault(size = 10) Pageable pageable) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        var page = dashboardService.historyExcludingHidden(userId, pageable);
        var response = PageResponse.from(page.map(TriageHistoryResponse::from));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{consultationId}")
    public ResponseEntity<TriageHistoryResponse> consultationDetail(
            Authentication authentication, @PathVariable UUID consultationId) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        TriageConsultation consultation = dashboardService.consultationDetail(userId, consultationId);
        return ResponseEntity.ok(TriageHistoryResponse.from(consultation));
    }

    @GetMapping("/profile")
    public ResponseEntity<PatientProfileResponse> profile(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(PatientProfileResponse.from(dashboardService.profile(userId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<PatientProfileResponse> updateProfile(
            Authentication authentication, @Valid @RequestBody PatientProfileRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== DASHBOARD PROFILE UPDATE === userId={}", userId);
        var profile = dashboardService.updateProfile(userId, request.riskFactors(), request.chronicConditions());
        return ResponseEntity.ok(PatientProfileResponse.from(profile));
    }

    @GetMapping("/care-plan")
    public ResponseEntity<CarePlanResponse> carePlan(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(CarePlanResponse.from(dashboardService.carePlan(userId)));
    }

    @PostMapping("/reminders")
    public ResponseEntity<ReminderResponse> createReminder(
            Authentication authentication, @Valid @RequestBody ReminderRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== DASHBOARD REMINDER CREATE === userId={}", userId);
        var reminder = com.kinplatform.kin.health.dashboard.domain.Reminder.of(
                UUID.randomUUID(),
                userId,
                parseType(request.type()),
                request.title(),
                request.scheduledAt(),
                true,
                null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReminderResponse.from(dashboardService.createReminder(userId, reminder)));
    }

    @GetMapping("/reminders")
    public ResponseEntity<List<ReminderResponse>> reminders(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(dashboardService.reminders(userId).stream()
                .map(ReminderResponse::from)
                .toList());
    }

    private com.kinplatform.kin.health.dashboard.domain.Reminder.ReminderType parseType(String raw) {
        try {
            return com.kinplatform.kin.health.dashboard.domain.Reminder.ReminderType.valueOf(
                    raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new com.kinplatform.kin.health.dashboard.api.InvalidReminderTypeException(raw);
        }
    }
}
