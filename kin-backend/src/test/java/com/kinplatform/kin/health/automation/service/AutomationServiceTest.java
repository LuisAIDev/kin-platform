package com.kinplatform.kin.health.automation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.automation.config.AutomationProperties;
import com.kinplatform.kin.health.automation.domain.ActionType;
import com.kinplatform.kin.health.automation.domain.AutomationRule;
import com.kinplatform.kin.health.automation.domain.TriggerEvent;
import com.kinplatform.kin.health.automation.port.AutomationRuleRepository;
import com.kinplatform.kin.health.automation.port.RuleExecutionLogRepository;
import com.kinplatform.kin.health.followup.api.FollowUpService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests de seguridad y ownership del m\u00f3dulo de automatizaciones (P1-2/P1-3/P1-6):
 * RBAC m\u00e9dico/admin, listado restringido al propio m\u00e9dico y ownership en
 * update/toggle/delete.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutomationServiceTest {

    private static final UUID PHYSICIAN_A = UUID.randomUUID();
    private static final UUID PHYSICIAN_B = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private AutomationRuleRepository ruleRepository;
    @Mock
    private RuleExecutionLogRepository executionLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailSender emailSender;
    @Mock
    private FollowUpService followUpService;

    private AutomationService service;
    private AutomationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AutomationProperties();
        properties.setEnabled(true);
        service = new AutomationService(
                ruleRepository, executionLogRepository, properties,
                userRepository, auditService, emailSender, followUpService);
    }

    private User user(UUID id, UserRole role) {
        return User.builder()
                .id(id)
                .email(id + "@kin.com")
                .role(role)
                .build();
    }

    private AutomationRule rule(UUID ownerId) {
        return AutomationRule.of(
                "Regla de " + ownerId, "desc", TriggerEvent.TRIAGE_PERFORMED,
                "{\"field\":\"urgency\",\"operator\":\"EQ\",\"value\":\"HIGH\"}",
                ActionType.SEND_EMAIL, "{}", true, ownerId);
    }

    private void seedPhysician(UUID id) {
        lenient().when(userRepository.findById(id))
                .thenReturn(Optional.of(user(id, UserRole.PHYSICIAN)));
    }

    // ---------- P1-6: createRule ----------

    @Test
    void createRule_medico_deberiaCrear() {
        seedPhysician(PHYSICIAN_A);
        when(ruleRepository.save(any(AutomationRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(ruleRepository.countByCreatedByAndEnabled(PHYSICIAN_A, true)).thenReturn(0L);

        var rule = service.createRule(user(PHYSICIAN_A, UserRole.PHYSICIAN),
                "Nueva", "desc", TriggerEvent.TRIAGE_PERFORMED,
                "{\"field\":\"urgency\",\"operator\":\"EQ\",\"value\":\"HIGH\"}",
                ActionType.SEND_EMAIL, "{}");

        assertEquals(PHYSICIAN_A, rule.createdBy());
    }

    // ---------- P1-2: listRules ----------

    @Test
    void listRules_medicoSinParametro_deberiaListarSoloSusReglas() {
        when(ruleRepository.findByCreatedBy(PHYSICIAN_A))
                .thenReturn(List.of(rule(PHYSICIAN_A)));

        var rules = service.listRules(user(PHYSICIAN_A, UserRole.PHYSICIAN), null);

        assertEquals(1, rules.size());
        verify(ruleRepository).findByCreatedBy(PHYSICIAN_A);
    }

    @Test
    void listRules_medicoConPhysicianIdDeOtroMedico_deberiaIgnorarElParametro() {
        when(ruleRepository.findByCreatedBy(PHYSICIAN_A))
                .thenReturn(List.of(rule(PHYSICIAN_A)));

        var rules = service.listRules(user(PHYSICIAN_A, UserRole.PHYSICIAN), PHYSICIAN_B);

        assertEquals(1, rules.size());
        verify(ruleRepository).findByCreatedBy(PHYSICIAN_A);
        verify(ruleRepository, never()).findByCreatedBy(PHYSICIAN_B);
    }

    @Test
    void listRules_adminConPhysicianId_deberiaPoderListarDeOtroMedico() {
        when(ruleRepository.findByCreatedBy(PHYSICIAN_B))
                .thenReturn(List.of(rule(PHYSICIAN_B)));

        var rules = service.listRules(user(ADMIN, UserRole.ADMIN), PHYSICIAN_B);

        assertEquals(1, rules.size());
        verify(ruleRepository).findByCreatedBy(PHYSICIAN_B);
    }

    // ---------- P1-3: updateRule ----------

    @Test
    void updateRule_medicoDueno_deberiaPermitir() {
        var regla = rule(PHYSICIAN_A);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));
        when(ruleRepository.update(any(AutomationRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var updated = service.updateRule(user(PHYSICIAN_A, UserRole.PHYSICIAN), regla.id(),
                "Nuevo nombre", "desc", TriggerEvent.TRIAGE_PERFORMED,
                "{\"field\":\"urgency\",\"operator\":\"EQ\",\"value\":\"HIGH\"}",
                ActionType.SEND_EMAIL, "{}", true);

        assertEquals("Nuevo nombre", updated.name());
    }

    @Test
    void updateRule_medicoAjeno_deberiaLanzar() {
        var regla = rule(PHYSICIAN_B);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));

        assertThrows(SecurityException.class,
                () -> service.updateRule(user(PHYSICIAN_A, UserRole.PHYSICIAN), regla.id(),
                        "x", "x", TriggerEvent.TRIAGE_PERFORMED, "{}", ActionType.SEND_EMAIL, "{}", true));
    }

    @Test
    void updateRule_admin_deberiaPermitirReglaAjena() {
        var regla = rule(PHYSICIAN_B);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));
        when(ruleRepository.update(any(AutomationRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var updated = service.updateRule(user(ADMIN, UserRole.ADMIN), regla.id(),
                "Nuevo admin", "desc", TriggerEvent.TRIAGE_PERFORMED,
                "{\"field\":\"urgency\",\"operator\":\"EQ\",\"value\":\"HIGH\"}",
                ActionType.SEND_EMAIL, "{}", true);

        assertEquals("Nuevo admin", updated.name());
    }

    // ---------- P1-3: toggleRule ----------

    @Test
    void toggleRule_medicoDueno_deberiaPermitir() {
        var regla = rule(PHYSICIAN_A);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));
        when(ruleRepository.update(any(AutomationRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.toggleRule(user(PHYSICIAN_A, UserRole.PHYSICIAN), regla.id(), false);

        verify(ruleRepository).update(any(AutomationRule.class));
    }

    @Test
    void toggleRule_medicoAjeno_deberiaLanzar() {
        var regla = rule(PHYSICIAN_B);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));

        assertThrows(SecurityException.class,
                () -> service.toggleRule(user(PHYSICIAN_A, UserRole.PHYSICIAN), regla.id(), false));
    }

    // ---------- P1-3: deleteRule ----------

    @Test
    void deleteRule_medicoDueno_deberiaPermitir() {
        var regla = rule(PHYSICIAN_A);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));

        service.deleteRule(user(PHYSICIAN_A, UserRole.PHYSICIAN), regla.id());

        verify(ruleRepository).deleteById(regla.id());
    }

    @Test
    void deleteRule_medicoAjeno_deberiaLanzar() {
        var regla = rule(PHYSICIAN_B);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));

        assertThrows(SecurityException.class,
                () -> service.deleteRule(user(PHYSICIAN_A, UserRole.PHYSICIAN), regla.id()));
    }

    // ---------- P1-3: deleteRule admin ----------

    @Test
    void deleteRule_admin_deberiaPermitirReglaAjena() {
        var regla = rule(PHYSICIAN_B);
        when(ruleRepository.findById(regla.id())).thenReturn(Optional.of(regla));

        service.deleteRule(user(ADMIN, UserRole.ADMIN), regla.id());

        verify(ruleRepository).deleteById(regla.id());
    }

    // ---------- Sentinel: assertTrue utilizado para evitar import sin uso ----------

    @Test
    void sentinel_listadoNoVacio() {
        when(ruleRepository.findByCreatedBy(PHYSICIAN_A)).thenReturn(List.of(rule(PHYSICIAN_A)));
        assertTrue(!service.listRules(user(PHYSICIAN_A, UserRole.PHYSICIAN), null).isEmpty());
    }
}
