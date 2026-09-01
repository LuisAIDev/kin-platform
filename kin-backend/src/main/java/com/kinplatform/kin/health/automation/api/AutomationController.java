package com.kinplatform.kin.health.automation.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.automation.domain.*;
import com.kinplatform.kin.health.automation.service.AutomationService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health/automation")
public class AutomationController {

    private final AutomationService automationService;
    private final UserRepository userRepository;

    public AutomationController(AutomationService automationService, UserRepository userRepository) {
        this.automationService = automationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/rules")
    public ResponseEntity<AutomationRule> createRule(
            Authentication authentication, @Valid @RequestBody AutomationRuleRequest request) {
        User user = requirePhysicianOrAdmin(authentication);
        var rule = automationService.createRule(
                user,
                request.name(),
                request.description(),
                TriggerEvent.valueOf(request.triggerEvent()),
                request.conditions(),
                ActionType.valueOf(request.action()),
                request.actionParams());
        return ResponseEntity.ok(rule);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AutomationRuleResponse>> listRules(
            Authentication authentication, @RequestParam(required = false) UUID physicianId) {
        User user = requirePhysicianOrAdmin(authentication);
        var rules = automationService.listRules(user, physicianId);
        var responses = rules.stream()
                .map(r -> new AutomationRuleResponse(
                        r.id(),
                        r.name(),
                        r.description(),
                        r.triggerEvent().name(),
                        r.conditions(),
                        r.action().name(),
                        r.actionParams(),
                        r.enabled(),
                        r.createdAt(),
                        r.updatedAt()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AutomationRule> updateRule(
            Authentication authentication, @PathVariable UUID id, @Valid @RequestBody AutomationRuleRequest request) {
        User user = requirePhysicianOrAdmin(authentication);
        automationService.updateRule(
                user,
                id,
                request.name(),
                request.description(),
                TriggerEvent.valueOf(request.triggerEvent()),
                request.conditions(),
                ActionType.valueOf(request.action()),
                request.actionParams(),
                request.enabled());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<Void> toggleRule(
            Authentication authentication, @PathVariable UUID id, @RequestParam boolean enabled) {
        User user = requirePhysicianOrAdmin(authentication);
        automationService.toggleRule(user, id, enabled);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(Authentication authentication, @PathVariable UUID id) {
        User user = requirePhysicianOrAdmin(authentication);
        automationService.deleteRule(user, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/logs")
    public ResponseEntity<List<ExecutionLogResponse>> adminLogs(
            @RequestParam(required = false) UUID ruleId,
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) Boolean executed) {
        var logs = automationService.executionLogRepository.findByRuleIdAndExecuted(
                ruleId != null ? ruleId : UUID.randomUUID(), executed != null && executed);
        // Note: this is a simplified endpoint - real implementation would need proper query
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }

    // --- Records para requests/response ---

    private User requirePhysicianOrAdmin(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (user.getRole() != UserRole.PHYSICIAN && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Se requiere rol de médico o administrador para esta operación");
        }
        return user;
    }

    public record AutomationRuleRequest(
            String name,
            String description,
            String triggerEvent,
            String conditions,
            String action,
            String actionParams,
            boolean enabled) {}

    public record AutomationRuleResponse(
            UUID id,
            String name,
            String description,
            String triggerEvent,
            String conditions,
            String action,
            String actionParams,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {}

    public record ExecutionLogResponse(
            UUID id,
            UUID ruleId,
            UUID eventId,
            OffsetDateTime triggeredAt,
            boolean executed,
            String error,
            String details) {}
}
