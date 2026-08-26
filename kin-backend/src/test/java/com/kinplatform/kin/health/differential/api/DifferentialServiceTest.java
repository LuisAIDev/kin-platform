package com.kinplatform.kin.health.differential.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.differential.InMemoryDifferentialKnowledgeRepository;
import com.kinplatform.kin.health.differential.config.DifferentialProperties;
import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.differential.engine.DifferentialEngine;
import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DifferentialServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010002");
    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");

    private static DifferentialProperties properties(boolean enabled) {
        var props = new DifferentialProperties();
        props.setEnabled(enabled);
        props.setMaxItems(5);
        return props;
    }

    private static DifferentialEngine engine() {
        var catalog = new DifferentialCatalog(
                List.of(RiskFactor.of(UUID.randomUUID(), C1, "fumador", 0.4, "Tabaquismo")),
                List.of(RecommendedTest.of(UUID.randomUUID(), C1, "PCR respiratoria", "Detección viral")));
        return new DifferentialEngine(new InMemoryDifferentialKnowledgeRepository(catalog));
    }

    private static TriageEngine triageEngine() {
        var catalog = new TriageCatalog(
                List.of(com.kinplatform.kin.health.triage.domain.Symptom.of(S1, "fiebre", "T", "R50")),
                List.of(com.kinplatform.kin.health.triage.domain.Condition.of(
                        C1, "Gripe", "D", "J11", Severity.MODERADO, Urgency.MEDIA, "R")),
                List.of(com.kinplatform.kin.health.triage.domain.SymptomConditionRelation.of(S1, C1, 0.9, true)));
        return new TriageEngine(new InMemoryTriageKnowledgeRepository(catalog));
    }

    private DifferentialService service(boolean enabled) {
        return new DifferentialService(
                engine(), triageEngine(), new InMemoryTriageConsultationRepository(), properties(enabled));
    }

    @Test
    void fromConsultation_deberiaDevolverResultadoDeLaConsultaDelUsuario() {
        var repo = new InMemoryTriageConsultationRepository();
        var consultation = TriageConsultation.of(
                UUID.randomUUID(),
                USER_ID,
                List.of("fiebre"),
                List.of(new TriageConditionResult(
                        C1, "Gripe", "D", 0.8, Severity.MODERADO, Urgency.MEDIA, "R", List.of("fiebre"))),
                OffsetDateTime.now());
        repo.save(consultation);
        var service = new DifferentialService(engine(), triageEngine(), repo, properties(true));

        var result = service.fromConsultation(USER_ID, consultation.id(), java.util.Set.of("fumador"));

        assertFalse(result.isEmpty());
        assertEquals("Gripe", result.items().get(0).name());
        assertTrue(result.items().get(0).probability() > 0.8);
    }

    @Test
    void fromConsultation_deberiaRechazarConsultaDeOtroUsuario() {
        var repo = new InMemoryTriageConsultationRepository();
        var consultation = TriageConsultation.of(
                UUID.randomUUID(), UUID.randomUUID(), List.of("fiebre"), List.of(), OffsetDateTime.now());
        repo.save(consultation);
        var service = new DifferentialService(engine(), triageEngine(), repo, properties(true));

        assertThrows(
                DifferentialNotFoundException.class,
                () -> service.fromConsultation(USER_ID, consultation.id(), java.util.Set.of()));
    }

    @Test
    void fromConsultation_deberiaLanzarSiNoExiste() {
        assertThrows(DifferentialNotFoundException.class, () -> service(true)
                .fromConsultation(USER_ID, UUID.randomUUID(), java.util.Set.of()));
    }

    @Test
    void fromSymptoms_deberiaEjecutarTriajePrimero() {
        var result = service(true).fromSymptoms(List.of("fiebre"), java.util.Set.of());

        assertFalse(result.isEmpty());
        assertEquals("Gripe", result.items().get(0).name());
    }

    @Test
    void fromSymptoms_sinReconocidos_deberiaDevolverVacio() {
        var result = service(true).fromSymptoms(List.of("síntoma inexistente"), java.util.Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        assertThrows(DifferentialDisabledException.class, () -> service(false)
                .fromSymptoms(List.of("fiebre"), java.util.Set.of()));
        assertThrows(DifferentialDisabledException.class, () -> service(false)
                .fromConsultation(USER_ID, UUID.randomUUID(), java.util.Set.of()));
    }
}
