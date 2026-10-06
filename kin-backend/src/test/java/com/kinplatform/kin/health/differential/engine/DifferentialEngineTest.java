package com.kinplatform.kin.health.differential.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.engine.EnginePhase;
import com.kinplatform.common.engine.EngineType;
import com.kinplatform.kin.health.differential.InMemoryDifferentialKnowledgeRepository;
import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.kin.health.differential.domain.DifferentialInput;
import com.kinplatform.kin.health.differential.domain.PatientContext;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DifferentialEngineTest {

    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010002"); // Gripe
    private static final UUID C2 = UUID.fromString("22220000-0000-0000-0000-000000010010"); // Asma
    private static final UUID C3 = UUID.fromString("22220000-0000-0000-0000-000000010011"); // Neumonía

    private static TriageConditionResult condition(UUID id, String name, double probability) {
        return new TriageConditionResult(
                id, name, "Desc " + name, probability, Severity.MODERADO, Urgency.MEDIA, "Consultar.", List.of("tos"));
    }

    private static DifferentialCatalog catalog() {
        return new DifferentialCatalog(
                List.of(
                        RiskFactor.of(UUID.randomUUID(), C2, "fumador", 0.40, "Tabaquismo"),
                        RiskFactor.of(UUID.randomUUID(), C3, "fumador", 0.35, "Tabaquismo"),
                        RiskFactor.of(UUID.randomUUID(), C3, "edad avanzada", 0.30, "Mayores 65")),
                List.of(
                        RecommendedTest.of(UUID.randomUUID(), C2, "Espirometría", "Función pulmonar"),
                        RecommendedTest.of(UUID.randomUUID(), C3, "Radiografía de tórax", "Infiltrados")));
    }

    private DifferentialEngine engine() {
        return new DifferentialEngine(new InMemoryDifferentialKnowledgeRepository(catalog()));
    }

    private DifferentialInput input(PatientContext context) {
        return DifferentialInput.of(
                List.of("tos", "fiebre"),
                List.of(condition(C1, "Gripe", 0.5), condition(C2, "Asma", 0.3), condition(C3, "Neumonía", 0.2)),
                context);
    }

    @Test
    void evaluar_deberiaProducirItemsOrdenadosPorProbabilidad() {
        var result = engine().evaluate(input(PatientContext.empty()));

        assertFalse(result.isEmpty());
        assertEquals(3, result.items().size());
        assertEquals("Gripe", result.items().get(0).name());
        assertEquals(0.5, result.items().get(0).probability(), 0.001);
    }

    @Test
    void riesgoCoincidente_deberiaAumentarLaProbabilidad() {
        var result = engine().evaluate(input(PatientContext.of(java.util.Set.of("fumador"))));

        var asma = result.items().stream()
                .filter(i -> i.name().equals("Asma"))
                .findFirst()
                .orElseThrow();
        var neumonia = result.items().stream()
                .filter(i -> i.name().equals("Neumonía"))
                .findFirst()
                .orElseThrow();
        var gripe = result.items().stream()
                .filter(i -> i.name().equals("Gripe"))
                .findFirst()
                .orElseThrow();

        // p' = p + weight·(1-p): Asma 0.3 + 0.4·0.7 = 0.58; Neumonía 0.2 + 0.35·0.8 = 0.48
        assertEquals(0.58, asma.probability(), 0.001);
        assertEquals(0.48, neumonia.probability(), 0.001);
        assertEquals(0.5, gripe.probability(), 0.001);
        assertEquals("Asma", result.items().get(0).name());
    }

    @Test
    void riesgoCoincidente_deberiaRegistrarElFactorEnElItem() {
        var result = engine().evaluate(input(PatientContext.of(java.util.Set.of("fumador"))));

        var asma = result.items().stream()
                .filter(i -> i.name().equals("Asma"))
                .findFirst()
                .orElseThrow();
        assertFalse(asma.riskFactors().isEmpty());
        assertEquals("fumador", asma.riskFactors().get(0).factor());
        assertTrue(asma.reasoning().contains("fumador"));
    }

    @Test
    void pruebasRecomendadas_deberianAdjuntarseALasCondiciones() {
        var result = engine().evaluate(input(PatientContext.empty()));

        var asma = result.items().stream()
                .filter(i -> i.name().equals("Asma"))
                .findFirst()
                .orElseThrow();
        assertFalse(asma.recommendedTests().isEmpty());
        assertEquals("Espirometría", asma.recommendedTests().get(0).test());
        var neumonia = result.items().stream()
                .filter(i -> i.name().equals("Neumonía"))
                .findFirst()
                .orElseThrow();
        assertFalse(neumonia.recommendedTests().isEmpty());
    }

    @Test
    void sinRiesgoCoincidente_deberiaMantenerProbabilidadBase() {
        var result = engine().evaluate(input(PatientContext.of(java.util.Set.of("embarazo"))));

        var asma = result.items().stream()
                .filter(i -> i.name().equals("Asma"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.3, asma.probability(), 0.001);
        assertTrue(asma.riskFactors().isEmpty());
    }

    @Test
    void entradaVacia_oNula_deberiaDevolverVacio() {
        var engine = engine();

        assertTrue(engine.evaluate(null).isEmpty());
        assertTrue(engine.evaluate(DifferentialInput.of(List.of(), List.of())).isEmpty());
    }

    @Test
    void maxItems_deberiaLimitarElResultado() {
        var engine = new DifferentialEngine(new InMemoryDifferentialKnowledgeRepository(catalog()), 2);

        var result = engine.evaluate(input(PatientContext.empty()));

        assertEquals(2, result.items().size());
    }

    @Test
    void resultado_deberiaTrazarGeneradorYVersion() {
        var result = engine().evaluate(input(PatientContext.empty()));

        assertEquals(DifferentialEngine.GENERATOR_NAME, result.generatedBy());
        assertEquals(DifferentialEngine.ENGINE_VERSION, result.engineVersion());
        assertNotNull(result.explanation());
    }

    @Test
    void metadata_deberiaExponerElMotor() {
        var metadata = engine().metadata();

        assertEquals(DifferentialEngine.GENERATOR_NAME, metadata.name());
        assertEquals(EnginePhase.VALIDATION, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
    }

    @Test
    void repositorioNulo_deberiaRechazarElMotor() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new DifferentialEngine(null));
    }
}

