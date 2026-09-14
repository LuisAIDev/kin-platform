package com.kinplatform.kin.health.triage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.triage.config.TriageProperties;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TriageServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private static TriageProperties properties(boolean enabled) {
        var props = new TriageProperties();
        props.setEnabled(enabled);
        props.setMaxConditions(5);
        return props;
    }

    private static InMemoryTriageKnowledgeRepository repo() {
        var fiebre = Symptom.of(
                UUID.fromString("22220000-0000-0000-0000-000000000001"), "fiebre", "Temperatura elevada", "R50.9");
        var gripe = Condition.of(
                UUID.fromString("22220000-0000-0000-0000-000000010001"),
                "Gripe",
                "Infección viral",
                "J11",
                Severity.MODERADO,
                Urgency.MEDIA,
                "Consulta médica.");
        return new InMemoryTriageKnowledgeRepository(new TriageCatalog(
                List.of(fiebre),
                List.of(gripe),
                List.of(SymptomConditionRelation.of(fiebre.id(), gripe.id(), 0.9, true))));
    }

    private static HealthQuotaPort healthQuotaPort() {
        return new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return null;
            }

            @Override
            public Integer getMaxPatients(UUID physicianId) {
                return null;
            }

            @Override
            public Integer getTrialDays(UUID userId) {
                return null;
            }

            @Override
            public Integer getTriagesUsed(UUID userId) {
                return 0;
            }
        };
    }

    private static UserRepository userRepository(UUID userId, boolean unlimited) {
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .fullName("Test User")
                .role(UserRole.PATIENT)
                .unlimitedAccess(unlimited)
                .build();
        UserRepository mock = mock(UserRepository.class);
        when(mock.findById(userId)).thenReturn(Optional.of(user));
        when(mock.findById(UUID.randomUUID())).thenReturn(Optional.empty());
        return mock;
    }

    private static TriageService service(boolean enabled, UserRepository userRepo) {
        var knowledge = repo();
        return new TriageService(
                new TriageEngine(knowledge),
                knowledge,
                new InMemoryTriageConsultationRepository(),
                properties(enabled),
                healthQuotaPort(),
                userRepo);
    }

    @Test
    void analyze_deberiaPersistirLaConsulta() {
        var knowledge = repo();
        var history = new InMemoryTriageConsultationRepository();
        var service = new TriageService(new TriageEngine(knowledge), knowledge, history, properties(true), healthQuotaPort(), mock(UserRepository.class));

        var result = service.analyze(USER_ID, List.of("fiebre"));

        assertFalse(result.isEmpty());
        assertEquals(1, history.all().size());
        assertEquals(USER_ID, history.all().get(0).userId());
        assertEquals(List.of("fiebre"), history.all().get(0).symptoms());
    }

    @Test
    void analyze_conModuloDeshabilitado_deberiaLanzar() {
        assertThrows(TriageDisabledException.class, () -> service(false, mock(UserRepository.class)).analyze(USER_ID, List.of("fiebre")));
    }

    @Test
    void analyze_sinUserId_deberiaRechazar() {
        assertThrows(IllegalArgumentException.class, () -> service(true, mock(UserRepository.class)).analyze(null, List.of("fiebre")));
    }

    @Test
    void listSymptoms_deberiaDevolverElCatalogo() {
        assertFalse(service(true, mock(UserRepository.class)).listSymptoms().isEmpty());
    }

    @Test
    void listSymptoms_conModuloDeshabilitado_deberiaLanzar() {
        assertThrows(TriageDisabledException.class, () -> service(false, mock(UserRepository.class)).listSymptoms());
    }

    @Test
    void history_deberiaDevolverSoloLasConsultasDelUsuario() {
        var knowledge = repo();
        var history = new InMemoryTriageConsultationRepository();
        var service = new TriageService(new TriageEngine(knowledge), knowledge, history, properties(true), healthQuotaPort(), mock(UserRepository.class));
        service.analyze(USER_ID, List.of("fiebre"));
        service.analyze(UUID.randomUUID(), List.of("fiebre"));

        assertEquals(1, service.history(USER_ID).size());
        assertTrue(service.history(null).isEmpty());
    }

    @Test
    void analyze_usuarioConUnlimitedAccess_deberiaSaltarCuota() {
        var knowledge = repo();
        var history = new InMemoryTriageConsultationRepository();
        var userRepo = userRepository(USER_ID, true);
        var quotaPort = new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return 3; // Límite normal
            }
            @Override
            public Integer getMaxPatients(UUID physicianId) { return null; }
            @Override
            public Integer getTrialDays(UUID userId) { return null; }
            @Override
            public Integer getTriagesUsed(UUID userId) { return 0; }
        };
        var service = new TriageService(
                new TriageEngine(knowledge), knowledge, history, properties(true), quotaPort, userRepo);

        // Simular 4 triajes (más del límite de 3)
        for (int i = 0; i < 4; i++) {
            service.analyze(USER_ID, List.of("fiebre"));
        }

        assertEquals(4, history.all().size());
    }

    @Test
    void analyze_usuarioSinUnlimitedAccess_deberiaRespetarCuota() {
        var knowledge = repo();
        var history = new InMemoryTriageConsultationRepository();
        var userRepo = userRepository(USER_ID, false);
        var quotaPort = new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return 3;
            }
            @Override
            public Integer getMaxPatients(UUID physicianId) { return null; }
            @Override
            public Integer getTrialDays(UUID userId) { return null; }
            @Override
            public Integer getTriagesUsed(UUID userId) { return 0; }
        };
        var service = new TriageService(
                new TriageEngine(knowledge), knowledge, history, properties(true), quotaPort, userRepo);

        // 3 triajes deberían funcionar
        service.analyze(USER_ID, List.of("fiebre"));
        service.analyze(USER_ID, List.of("fiebre"));
        service.analyze(USER_ID, List.of("fiebre"));

        // El 4to debería lanzar
        assertThrows(QuotaExceededException.class, () -> service.analyze(USER_ID, List.of("fiebre")));
    }
}