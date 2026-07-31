package com.kinplatform.kin.engine;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EngineRegistryTest {

    record TestInput(String value) implements EngineInput {
        public ProjectContext projectContext() { return null; }
        public CompletenessEvaluation evaluation() { return null; }
        public ConversationDecision decision() { return null; }
        public ScoreResult score() { return null; }
    }

    record TestResult(String value) implements EngineResult {
        public double confidence() { return 1.0; }
        public String explanation() { return value; }
        public String generatedBy() { return "TestEngine"; }
        public String engineVersion() { return "v1"; }
        public boolean isEmpty() { return value == null; }
    }

    static final class FakeEngine implements DomainEngine<TestInput, TestResult> {
        private final EngineMetadata metadata;
        private final String output;

        FakeEngine(String name, int priority, EnginePhase phase, String output) {
            this.metadata = EngineMetadata.of(name, "v1", "Test", phase, EngineType.DOMAIN, priority);
            this.output = output;
        }

        public EngineMetadata metadata() { return metadata; }
        public TestResult evaluate(TestInput input) { return new TestResult(output); }
    }

    private final FakeEngine recommendation = new FakeEngine("RecommendationEngine", 40,
        EnginePhase.RECOMMENDATION, "rec");
    private final FakeEngine risk = new FakeEngine("RiskEngine", 50, EnginePhase.RISK, "risk");
    private final FakeEngine opportunity = new FakeEngine("OpportunityEngine", 30,
        EnginePhase.OPPORTUNITY, "opp");
    private final EngineRegistry registry = new EngineRegistry(List.of(recommendation, risk, opportunity));

    @Test
    void constructor_deberiaIndexarPorNombre() {
        assertEquals(3, registry.size());
    }

    @Test
    void find_deberiaDevolverElMotor_cuandoExiste() {
        assertTrue(registry.find("RiskEngine").isPresent());
        assertEquals(EnginePhase.RISK, registry.find("RiskEngine").get().metadata().phase());
    }

    @Test
    void find_deberiaDevolverVacio_cuandoNoExiste() {
        assertTrue(registry.find("NoExiste").isEmpty());
    }

    @Test
    void contains_deberiaDetectarPresencia() {
        assertTrue(registry.contains("OpportunityEngine"));
        assertFalse(registry.contains("OtroEngine"));
    }

    @Test
    void names_deberiaDevolverLosNombresRegistrados() {
        assertEquals(3, registry.names().size());
        assertTrue(registry.names().containsAll(List.of("RecommendationEngine", "RiskEngine", "OpportunityEngine")));
    }

    @Test
    void allOrdered_deberiaOrdenarPorFaseYPrioridad() {
        var ordered = registry.allOrdered();
        assertEquals(List.of("RecommendationEngine", "RiskEngine", "OpportunityEngine"),
            ordered.stream().map(e -> e.metadata().name()).toList());
    }

    @Test
    void byPhase_deberiaFiltrarPorFase() {
        var risks = registry.byPhase(EnginePhase.RISK);
        assertEquals(1, risks.size());
        assertEquals("RiskEngine", risks.get(0).metadata().name());
        assertTrue(registry.byPhase(EnginePhase.SCORING).isEmpty());
    }

    @Test
    void after_deberiaDevolverLosMotoresPosteriores() {
        var after = registry.after("RecommendationEngine");
        assertEquals(List.of("RiskEngine", "OpportunityEngine"),
            after.stream().map(e -> e.metadata().name()).toList());
    }

    @Test
    void after_deberiaDevolverVacio_cuandoMotorNoExiste() {
        assertTrue(registry.after("NoExiste").isEmpty());
    }
}
