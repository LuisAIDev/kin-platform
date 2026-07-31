package com.kinplatform.kin.engine;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.scoring.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EngineExecutorTest {

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

    private final EngineExecutor executor = new EngineExecutor();

    @Test
    void execute_deberiaDevolverResultadoMetadatosYRuntime() {
        var engine = new FakeEngine("A", 10, EnginePhase.SCORING, "salida");
        var execution = executor.execute(engine, new TestInput("in"));

        assertEquals("salida", execution.result().value());
        assertEquals("A", execution.metadata().name());
        assertTrue(execution.runtimeMs() >= 0);
    }

    @Test
    void executeAll_deberiaEjecutarEnOrdenDePrioridad() {
        var low = new FakeEngine("Baja", 100, EnginePhase.RISK, "baja");
        var high = new FakeEngine("Alta", 10, EnginePhase.RISK, "alta");
        var results = executor.executeAll(List.of(low, high), engine -> new TestInput("in"));

        assertEquals(List.of("alta", "baja"),
            results.stream().map(e -> e.result().value()).toList());
        assertEquals(List.of("Alta", "Baja"),
            results.stream().map(e -> e.metadata().name()).toList());
    }

    @Test
    void executeIf_deberiaEjecutar_cuandoCondicionSeCumple() {
        var engine = new FakeEngine("A", 10, EnginePhase.SCORING, "salida");
        Optional<EngineExecution<TestResult>> execution = executor.executeIf(engine,
            new TestInput("in"), metadata -> metadata.priority() < 50);

        assertTrue(execution.isPresent());
        assertEquals("salida", execution.get().result().value());
    }

    @Test
    void executeIf_deberiaOmitir_cuandoCondicionNoSeCumple() {
        var engine = new FakeEngine("A", 90, EnginePhase.SCORING, "salida");
        Optional<EngineExecution<TestResult>> execution = executor.executeIf(engine,
            new TestInput("in"), metadata -> metadata.priority() < 50);

        assertTrue(execution.isEmpty());
    }

    @Test
    void executeOptional_deberiaEjecutar_cuandoEntradaNoEsNula() {
        var engine = new FakeEngine("A", 10, EnginePhase.SCORING, "salida");
        Optional<EngineExecution<TestResult>> execution = executor.executeOptional(engine,
            () -> new TestInput("in"));

        assertTrue(execution.isPresent());
        assertEquals("salida", execution.get().result().value());
    }

    @Test
    void executeOptional_deberiaOmitir_cuandoEntradaEsNula() {
        var engine = new FakeEngine("A", 10, EnginePhase.SCORING, "salida");
        Optional<EngineExecution<TestResult>> execution = executor.executeOptional(engine, () -> null);

        assertTrue(execution.isEmpty());
    }

    @Test
    void executeAllParallel_deberiaDelegarEnSecuencial() {
        var a = new FakeEngine("A", 40, EnginePhase.RISK, "a");
        var b = new FakeEngine("B", 30, EnginePhase.RISK, "b");
        var results = executor.executeAllParallel(List.of(a, b), engine -> new TestInput("in"));

        assertEquals(List.of("b", "a"), results.stream().map(e -> e.result().value()).toList());
    }
}
