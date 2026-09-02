package com.kinplatform.kin.health.triage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.List;
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
        };
    }

    private static TriageService service(boolean enabled) {
        var knowledge = repo();
        return new TriageService(
                new TriageEngine(knowledge),
                knowledge,
                new InMemoryTriageConsultationRepository(),
                properties(enabled),
                healthQuotaPort());
    }

    @Test
    void analyze_deberiaPersistirLaConsulta() {
        var knowledge = repo();
        var history = new InMemoryTriageConsultationRepository();
        var service = new TriageService(new TriageEngine(knowledge), knowledge, history, properties(true), healthQuotaPort());

        var result = service.analyze(USER_ID, List.of("fiebre"));

        assertFalse(result.isEmpty());
        assertEquals(1, history.all().size());
        assertEquals(USER_ID, history.all().get(0).userId());
        assertEquals(List.of("fiebre"), history.all().get(0).symptoms());
    }

    @Test
    void analyze_conModuloDeshabilitado_deberiaLanzar() {
        assertThrows(TriageDisabledException.class, () -> service(false).analyze(USER_ID, List.of("fiebre")));
    }

    @Test
    void analyze_sinUserId_deberiaRechazar() {
        assertThrows(IllegalArgumentException.class, () -> service(true).analyze(null, List.of("fiebre")));
    }

    @Test
    void listSymptoms_deberiaDevolverElCatalogo() {
        assertFalse(service(true).listSymptoms().isEmpty());
    }

    @Test
    void listSymptoms_conModuloDeshabilitado_deberiaLanzar() {
        assertThrows(TriageDisabledException.class, () -> service(false).listSymptoms());
    }

    @Test
    void history_deberiaDevolverSoloLasConsultasDelUsuario() {
        var knowledge = repo();
        var history = new InMemoryTriageConsultationRepository();
        var service = new TriageService(new TriageEngine(knowledge), knowledge, history, properties(true), healthQuotaPort());
        service.analyze(USER_ID, List.of("fiebre"));
        service.analyze(UUID.randomUUID(), List.of("fiebre"));

        assertEquals(1, service.history(USER_ID).size());
        assertTrue(service.history(null).isEmpty());
    }
}