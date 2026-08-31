package com.kinplatform.kin.health.automation.service;

import com.kinplatform.kin.health.automation.config.AutomationProperties;
import com.kinplatform.kin.health.automation.domain.*;
import com.kinplatform.kin.health.automation.port.*;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.kin.health.followup.api.FollowUpService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AutomationService {

    private static final Logger log = LoggerFactory.getLogger(AutomationService.class);

    private final AutomationRuleRepository ruleRepository;
    public final RuleExecutionLogRepository executionLogRepository;
    private final AutomationProperties properties;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailSender emailSender;
    private final FollowUpService followUpService;

    // --- Gestión de reglas ---

    public AutomationRule createRule(User actor, String name, String description,
                                     TriggerEvent triggerEvent, String conditions,
                                     ActionType action, String actionParams) {
        var physicianId = actor.getId();
        var user = userRepository.findById(physicianId)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado: " + physicianId));

        if (!properties.isEnabled()) {
            throw new IllegalStateException("Módulo de automatizaciones deshabilitado");
        }

        if (countRulesByPhysician(physicianId) >= properties.getMaxRulesPerPhysician()) {
            throw new IllegalStateException("Límite de reglas alcanzado para este médico (" +
                    properties.getMaxRulesPerPhysician() + ")");
        }

        var rule = AutomationRule.of(name, description, triggerEvent, conditions, action, actionParams,
                true, physicianId);

        var saved = ruleRepository.save(rule);

        auditLog(AuditAction.CREATE, AuditResourceType.AUTOMATION_RULE, saved.id(), physicianId,
                Map.of("triggerEvent", triggerEvent.name(), "action", action.name()));

        return saved;
    }

    public AutomationRule updateRule(User actor, UUID ruleId, String name, String description,
                                     TriggerEvent triggerEvent, String conditions,
                                     ActionType action, String actionParams, boolean enabled) {
        var rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada: " + ruleId));

        // Validar que el médico solo puede editar sus propias reglas
        if (!rule.createdBy().equals(actor.getId()) && !isAdmin(actor)) {
            throw new SecurityException("No tiene permiso para editar esta regla");
        }

        var updated = AutomationRule.of(name, description, triggerEvent, conditions, action, actionParams,
                enabled, rule.createdBy());

        var saved = ruleRepository.update(updated);

        auditLog(AuditAction.UPDATE, AuditResourceType.AUTOMATION_RULE, saved.id(), rule.createdBy(),
                Map.of("triggerEvent", triggerEvent.name(), "action", action.name()));

        return saved;
    }

    public void toggleRule(User actor, UUID ruleId, boolean enabled) {
        var rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada: " + ruleId));

        if (!rule.createdBy().equals(actor.getId()) && !isAdmin(actor)) {
            throw new SecurityException("No tiene permiso para activar/desactivar esta regla");
        }

        var updated = AutomationRule.of(
                rule.name(), rule.description(), rule.triggerEvent(), rule.conditions(),
                rule.action(), rule.actionParams(), enabled, rule.createdBy());

        ruleRepository.update(updated);

        auditLog(AuditAction.TOGGLE, AuditResourceType.AUTOMATION_RULE, rule.id(), rule.createdBy(),
                Map.of("enabled", enabled));
    }

    public List<AutomationRule> listRules(User actor, UUID requestedPhysicianId) {
        var physicianId = requestedPhysicianId != null && isAdmin(actor)
                ? requestedPhysicianId
                : actor.getId();
        return ruleRepository.findByCreatedBy(physicianId);
    }

    public void deleteRule(User actor, UUID ruleId) {
        var rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada: " + ruleId));

        if (!rule.createdBy().equals(actor.getId()) && !isAdmin(actor)) {
            throw new SecurityException("No tiene permiso para eliminar esta regla");
        }

        auditLog(AuditAction.DELETE, AuditResourceType.AUTOMATION_RULE, ruleId, rule.createdBy(),
                Map.of());

        ruleRepository.deleteById(ruleId);
    }

    public long countRulesByPhysician(UUID physicianId) {
        return ruleRepository.countByCreatedByAndEnabled(physicianId, true);
    }

    // --- Motor de evaluación ---

    @Transactional
    public void evaluateAndExecute(TriggerEvent event, UUID eventId, UUID eventPhysicianId,
                                   String eventPayloadJson) {
        var enabledRules = ruleRepository.findByTriggerEventAndEnabled(event, true);

        for (var rule : enabledRules) {
            if (!rule.enabled()) continue;

            // Verificar que el médico tiene permisos sobre el evento (solo si regla es del médico)
            if (!rule.createdBy().equals(eventPhysicianId)) {
                continue;
            }

            // Evaluar condiciones
            if (!evaluateConditions(rule.conditions(), eventPayloadJson)) {
                continue;
            }

            // Ejecutar acción
            try {
                executeAction(rule.action(), rule.actionParams(), eventPhysicianId, eventId, rule.createdBy());
                
                // Registrar ejecución exitosa
                var log = RuleExecutionLog.of(rule.id(), eventId);
                executionLogRepository.save(log);

                auditLog(AuditAction.EXECUTE, AuditResourceType.AUTOMATION_RULE_EXECUTION,
                        log.id(), rule.createdBy(),
                        Map.of("ruleId", rule.id().toString(), "eventId", eventId.toString(),
                                "action", rule.action().name()));
            } catch (Exception e) {
                // Registrar ejecución fallida
                var executionLog = RuleExecutionLog.of(rule.id(), eventId);
                executionLog = new RuleExecutionLog(executionLog.id(), executionLog.ruleId(), executionLog.eventId(), executionLog.triggeredAt(),
                        false, e.getMessage(), null);
                executionLogRepository.save(executionLog);

                log.error("Error executing automation rule {}: {}", rule.id(), e.getMessage());
            }
        }
    }

    private boolean evaluateConditions(String conditionsJson, String eventPayloadJson) {
        try {
            // Parsear condiciones simples: {"field": "urgency", "operator": "EQ", "value": "HIGH"}
            var conditions = parseJson(conditionsJson);
            var payload = parseJson(eventPayloadJson);

            var field = getJsonValue(conditions, "field");
            var operator = getJsonValue(conditions, "operator");
            var expectedValue = getJsonValue(conditions, "value");

            if (field == null || operator == null || expectedValue == null) {
                return false;
            }

            var actualValue = getFieldFromPayload(payload, field);

            return compareValues(actualValue, operator, expectedValue);
        } catch (Exception e) {
            log.warn("Error evaluating conditions: {}", e.getMessage());
            return false;
        }
    }

    private String getFieldFromPayload(Object payload, String field) {
        // Navegación simple por puntos (ej. "urgency" o "patient.age")
        if (field.contains(".")) {
            var parts = field.split("\\.");
            var obj = payload;
            for (var part : parts) {
                if (obj instanceof java.util.Map) {
                    obj = ((java.util.Map<?, ?>) obj).get(part);
                } else {
                    return null;
                }
            }
            return obj != null ? obj.toString() : null;
        } else {
            if (payload instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) payload).get(field) != null ?
                        ((java.util.Map<?, ?>) payload).get(field).toString() : null;
            }
            return null;
        }
    }

    private boolean compareValues(String actual, String operator, String expected) {
        switch (operator.toUpperCase()) {
            case "EQ":
                return actual.equals(expected);
            case "NE":
                return !actual.equals(expected);
            case "GT":
                return doubleCompare(actual, expected) > 0;
            case "GTE":
                return doubleCompare(actual, expected) >= 0;
            case "LT":
                return doubleCompare(actual, expected) < 0;
            case "LTE":
                return doubleCompare(actual, expected) <= 0;
            default:
                return false;
        }
    }

    private double doubleCompare(String a, String b) {
        try {
            return Double.parseDouble(a) - Double.parseDouble(b);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Object parseJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("Invalid JSON: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String getJsonValue(Object obj, String key) {
        if (obj instanceof java.util.Map) {
            return ((java.util.Map<String, Object>) obj).get(key) != null ?
                    ((java.util.Map<String, Object>) obj).get(key).toString() : null;
        }
        return null;
    }

    // --- Ejecutar acciones ---

    @Transactional
    private void executeAction(ActionType action, String actionParams, UUID physicianId,
                               UUID eventId, UUID executedBy) {
        switch (action) {
            case CREATE_ALERT:
                executeCreateAlert(actionParams, physicianId, eventId, executedBy);
                break;
            case SEND_NOTIFICATION:
                executeSendNotification(actionParams, physicianId, eventId, executedBy);
                break;
            case SEND_EMAIL:
                executeSendEmail(actionParams, physicianId, eventId, executedBy);
                break;
            case CREATE_TASK:
                executeCreateTask(actionParams, physicianId, eventId, executedBy);
                break;
            default:
                throw new IllegalArgumentException("Acción no soportada: " + action);
        }
    }

    private void executeCreateAlert(String params, UUID physicianId, UUID eventId, UUID executedBy) {
        throw new UnsupportedOperationException("CREATE_ALERT no implementado: AlertService no disponible");
    }

    private void executeSendNotification(String params, UUID physicianId, UUID eventId, UUID executedBy) {
        throw new UnsupportedOperationException("SEND_NOTIFICATION no implementado: NotificationService no disponible");
    }

    private void executeSendEmail(String params, UUID physicianId, UUID eventId, UUID executedBy) {
        throw new UnsupportedOperationException("SEND_EMAIL no implementado: EmailSender contract not compatible");
    }

    private void executeCreateTask(String params, UUID physicianId, UUID eventId, UUID executedBy) {
        throw new UnsupportedOperationException(
                "CREATE_TASK no implementado: requiere un planId valido de FollowUpPlan no resoluble desde el dominio de Automation");
    }

    // --- Utilidades ---

    private boolean isAdmin(User actor) {
        return actor != null && actor.getRole() == com.kinplatform.user.UserRole.ADMIN;
    }

    private void auditLog(AuditAction action, AuditResourceType resource, UUID resourceId,
                          UUID userId, java.util.Map<String, Object> details) {
        if (auditService != null) {
            auditService.logAccess(userId, action, resource, resourceId, userId, details);
        }
    }
}