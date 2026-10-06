package com.kinplatform.kin.health.triage.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.engine.EnginePhase;
import com.kinplatform.common.engine.EngineType;
import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageInput;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TriageEngineTest {

    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");
    private static final UUID S3 = UUID.fromString("22220000-0000-0000-0000-000000000003");
    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010001");
    private static final UUID C2 = UUID.fromString("22220000-0000-0000-0000-000000010002");

    private static Symptom symptom(UUID id, String name) {
        return Symptom.of(id, name, "Descripción " + name, null);
    }

    private static Condition condition(UUID id, String name) {
        return Condition.of(
                id, name, "Descripción " + name, "X00", Severity.MODERADO, Urgency.MEDIA, "Consultar médico.");
    }

    private static TriageKnowledgeRepository repo(TriageCatalog catalog) {
        return new InMemoryTriageKnowledgeRepository(catalog);
    }

    private TriageCatalog catalog() {
        var fiebre = symptom(S1, "fiebre");
        var tos = symptom(S2, "tos");
        var garganta = symptom(S3, "dolor de garganta");
        var gripe = condition(C1, "Gripe");
        var resfriado = condition(C2, "Resfriado común");
        return new TriageCatalog(
                List.of(fiebre, tos, garganta),
                List.of(gripe, resfriado),
                List.of(
                        SymptomConditionRelation.of(S1, C1, 0.9, true),
                        SymptomConditionRelation.of(S2, C1, 0.6, false),
                        SymptomConditionRelation.of(S3, C1, 0.5, false),
                        SymptomConditionRelation.of(S2, C2, 0.7, false),
                        SymptomConditionRelation.of(S3, C2, 0.6, false)));
    }

    @Test
    void evaluarConSintomasEspecificos_deberiaOrdenarPorProbabilidadDescendente() {
        var engine = new TriageEngine(repo(catalog()));
        var result = engine.evaluate(TriageInput.of(List.of("fiebre", "tos", "dolor de garganta")));

        assertFalse(result.isEmpty());
        assertEquals(2, result.results().size());
        assertEquals("Gripe", result.results().get(0).name());
        assertEquals("Resfriado común", result.results().get(1).name());
        assertTrue(
                result.results().get(0).probability() > result.results().get(1).probability());
    }

    @Test
    void sintomaObligatorioAusente_deberiaExcluirLaCondicion() {
        var engine = new TriageEngine(repo(catalog()));
        // Gripe requiere "fiebre" (required=true). Sin ella, solo Resfriado.
        var result = engine.evaluate(TriageInput.of(List.of("tos", "dolor de garganta")));

        assertFalse(result.isEmpty());
        assertEquals(1, result.results().size());
        assertEquals("Resfriado común", result.results().get(0).name());
    }

    @Test
    void sinSintomasReconocidos_deberiaDevolverVacio() {
        var engine = new TriageEngine(repo(catalog()));
        var result = engine.evaluate(TriageInput.of(List.of("síntoma inexistente")));

        assertTrue(result.isEmpty());
    }

    @Test
    void entradaVacia_oNula_deberiaDevolverVacio() {
        var engine = new TriageEngine(repo(catalog()));

        assertTrue(engine.evaluate(TriageInput.of(List.of())).isEmpty());
        assertTrue(engine.evaluate(null).isEmpty());
    }

    @Test
    void catalogoVacio_deberiaDevolverVacio() {
        var engine = new TriageEngine(repo(TriageCatalog.empty()));
        var result = engine.evaluate(TriageInput.of(List.of("fiebre")));

        assertTrue(result.isEmpty());
    }

    @Test
    void probabilidades_deberianSumarUno() {
        var engine = new TriageEngine(repo(catalog()));
        var result = engine.evaluate(TriageInput.of(List.of("fiebre", "tos", "dolor de garganta")));

        double sum = result.results().stream().mapToDouble(r -> r.probability()).sum();
        assertEquals(1.0, sum, 0.01);
    }

    @Test
    void maxConditions_deberiaLimitarElResultado() {
        var engine = new TriageEngine(repo(catalog()), 1);
        var result = engine.evaluate(TriageInput.of(List.of("fiebre", "tos", "dolor de garganta")));

        assertEquals(1, result.results().size());
    }

    @Test
    void matchedSymptoms_deberiaReflejarLasCoincidencias() {
        var engine = new TriageEngine(repo(catalog()));
        var result = engine.evaluate(TriageInput.of(List.of("fiebre", "tos")));

        var gripe = result.results().stream()
                .filter(r -> r.name().equals("Gripe"))
                .findFirst()
                .orElseThrow();
        assertTrue(gripe.matchedSymptoms().contains("fiebre"));
        assertTrue(gripe.matchedSymptoms().contains("tos"));
        assertFalse(gripe.matchedSymptoms().contains("dolor de garganta"));
    }

    @Test
    void sintomaPorId_deberiaResolverIgualQuePorNombre() {
        var engine = new TriageEngine(repo(catalog()));
        var byName = engine.evaluate(TriageInput.of(List.of("fiebre", "tos")));
        var byId = engine.evaluate(TriageInput.of(List.of(S1.toString(), S2.toString())));

        assertEquals(byName.results().size(), byId.results().size());
        assertEquals(byName.results().get(0).name(), byId.results().get(0).name());
    }

    @Test
    void resultado_deberiaTrazarGeneradorYVersion() {
        var engine = new TriageEngine(repo(catalog()));
        var result = engine.evaluate(TriageInput.of(List.of("fiebre")));

        assertEquals(TriageEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(TriageEngine.ENGINE_VERSION, result.engineVersion());
        assertNotNull(result.explanation());
        assertEquals(1.0, result.confidence(), 0.01);
    }

    @Test
    void metadata_deberiaExponerElMotor() {
        var metadata = new TriageEngine(repo(catalog())).metadata();

        assertEquals(TriageEngine.GENERATOR_NAME, metadata.name());
        assertEquals(TriageEngine.ENGINE_VERSION, metadata.version());
        assertEquals(EnginePhase.VALIDATION, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void repositorioNulo_deberiaRechazarElMotor() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new TriageEngine(null));
    }
}

