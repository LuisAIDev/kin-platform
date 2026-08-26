package com.kinplatform.kin.health.triage.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.pipeline.stage.AnalyzerStage;
import com.kinplatform.kin.pipeline.stage.EvaluatorStage;
import com.kinplatform.kin.pipeline.stage.StrategistStage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Integración de la {@link TriageStage} en el pipeline (ADR-028).
 *
 * <p>Verifica que la etapa se ejecuta en el orden esperado (después del
 * Analizador) y que almacena el {@code triageResult} en el contexto sin
 * romper las etapas previas.</p>
 */
class TriageStagePipelineTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static TriageKnowledgeRepository repo() {
        var fiebre = Symptom.of(
                UUID.fromString("22220000-0000-0000-0000-000000000001"), "fiebre", "Temperatura elevada", "R50.9");
        var tos = Symptom.of(UUID.fromString("22220000-0000-0000-0000-000000000002"), "tos", "Tos", null);
        var gripe = Condition.of(
                UUID.fromString("22220000-0000-0000-0000-000000010001"),
                "Gripe",
                "Infección viral",
                "J11",
                Severity.MODERADO,
                Urgency.MEDIA,
                "Consulta médica.");
        return new InMemoryTriageKnowledgeRepository(new TriageCatalog(
                List.of(fiebre, tos),
                List.of(gripe),
                List.of(
                        SymptomConditionRelation.of(fiebre.id(), gripe.id(), 0.9, true),
                        SymptomConditionRelation.of(tos.id(), gripe.id(), 0.6, false))));
    }

    private static List<PipelineStage> stages(boolean triageEnabled, List<String> order) {
        var knowledge = repo();
        var triage = new TriageStage(new TriageEngine(knowledge), knowledge, triageEnabled);
        var pipelineStages = List.of(
                new AnalyzerStage((message, ctx) -> com.kinplatform.kin.context.AnalysisResult.empty()),
                triage,
                new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
                new StrategistStage(new ConversationStrategist(
                        new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))));
        return pipelineStages.stream().map(s -> record(s, order)).toList();
    }

    private static PipelineContext context(String message) {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, message, List.of(), "Mi salud", "Cuidado de salud", "SALUD");
        ctx.projectContext(fullContext());
        return ctx;
    }

    private static ProjectContext fullContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var dim : AnalyzedDimension.values()) {
            data.put(dim, dim.displayName().repeat(30));
        }
        return ProjectContext.restore(data, EnumSet.allOf(AnalyzedDimension.class), null, 5, false);
    }

    private static PipelineStage record(PipelineStage delegate, List<String> order) {
        return new PipelineStage() {
            @Override
            public String name() {
                return delegate.name();
            }

            @Override
            public boolean supports(PipelineContext context) {
                return delegate.supports(context);
            }

            @Override
            public PipelineContext execute(PipelineContext context) {
                order.add(delegate.name());
                return delegate.execute(context);
            }
        };
    }

    @Test
    void pipeline_deberiaEjecutarTriageEnElOrdenEsperado_yAlmacenarElResultado() {
        var order = new ArrayList<String>();
        var result = new Pipeline(stages(true, order)).execute(context("tengo fiebre y tos"));

        assertEquals(List.of("Analizador", "Triaje", "Evaluador", "Estratega"), order);
        assertNotNull(result.triageResult());
        assertFalse(result.triageResult().isEmpty());
        assertEquals("Gripe", result.triageResult().results().get(0).name());
        assertTrue(result.engineResults().containsKey(TriageEngine.GENERATOR_NAME));
    }

    @Test
    void pipeline_sinSintomas_deberiaOmitirTriage() {
        var order = new ArrayList<String>();
        var result = new Pipeline(stages(true, order)).execute(context("generá el informe"));

        // La etapa soporta el turno (proyecto SALUD) pero se omite sin síntomas:
        // no produce resultado de triaje ni emite evento.
        assertTrue(order.contains("Triaje"));
        assertTrue(result.triageResult() == null);
    }

    @Test
    void pipeline_conModuloDeshabilitado_deberiaOmitirTriage() {
        var order = new ArrayList<String>();
        var result = new Pipeline(stages(false, order)).execute(context("tengo fiebre"));

        assertFalse(order.contains("Triaje"));
        assertTrue(result.triageResult() == null);
    }
}
