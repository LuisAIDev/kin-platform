package com.kinplatform.kin.health.differential.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.kin.health.differential.InMemoryDifferentialKnowledgeRepository;
import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.differential.engine.DifferentialEngine;
import com.kinplatform.kin.health.differential.event.DifferentialPerformedEvent;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.common.pipeline.PipelineContext;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DifferentialStageTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010002");

    private static DifferentialStage stage(boolean enabled) {
        var catalog = new DifferentialCatalog(
                List.of(RiskFactor.of(UUID.randomUUID(), C1, "fumador", 0.4, "Tabaquismo")),
                List.of(RecommendedTest.of(UUID.randomUUID(), C1, "PCR respiratoria", "Detección viral")));
        return new DifferentialStage(
                new DifferentialEngine(new InMemoryDifferentialKnowledgeRepository(catalog)), enabled);
    }

    private static TriageResult triageResult() {
        return new TriageResult(
                List.of(new TriageConditionResult(
                        C1,
                        "Gripe",
                        "Infección viral",
                        0.6,
                        Severity.MODERADO,
                        Urgency.MEDIA,
                        "Consultar.",
                        List.of("fiebre", "tos"))),
                List.of("fiebre", "tos"),
                0.6,
                "triaje ok",
                "TriageEngine",
                "v1");
    }

    private static PipelineContext context() {
        var ctx = new PipelineContext(
                PROJECT_ID, USER_ID, "tengo fiebre y tos", List.of(), "Mi salud", "Cuidado de salud", "SALUD");
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.PROJECT_NAME, "Mi salud");
        ctx.projectContext(ProjectContext.restore(data, EnumSet.of(AnalyzedDimension.PROJECT_NAME), null, 1, false));
        ctx.triageResult(triageResult());
        return ctx;
    }

    @Test
    void nombre_deberiaSerDiagnosticoDiferencial() {
        assertEquals("Diagnóstico diferencial", stage(true).name());
    }

    @Test
    void supports_deberiaSerTrue_conTriageResultYModuloHabilitado() {
        assertTrue(stage(true).supports(context()));
    }

    @Test
    void supports_deberiaSerFalse_sinTriageResult() {
        var ctx = context();
        ctx.triageResult(null);
        assertFalse(stage(true).supports(ctx));
    }

    @Test
    void supports_deberiaSerFalse_conTriageVacio() {
        var ctx = context();
        ctx.triageResult(TriageResult.empty());
        assertFalse(stage(true).supports(ctx));
    }

    @Test
    void supports_deberiaSerFalse_conModuloDeshabilitado() {
        assertFalse(stage(false).supports(context()));
    }

    @Test
    void execute_deberiaProducirDifferentialResult() {
        var result = stage(true).execute(context());

        assertNotNull(result.differentialResult());
        assertFalse(result.differentialResult().isEmpty());
        assertEquals("Gripe", result.differentialResult().items().get(0).name());
        assertTrue(result.engineResults().containsKey(DifferentialEngine.GENERATOR_NAME));
    }

    @Test
    void execute_deberiaEmitirDifferentialPerformedEvent() {
        var result = stage(true).execute(context());

        boolean hasEvent = result.events().stream().anyMatch(e -> e instanceof DifferentialPerformedEvent);
        assertTrue(hasEvent);
    }

    @Test
    void execute_sinTriageResult_deberiaOmitirElMotor() {
        var ctx = context();
        ctx.triageResult(null);
        var result = stage(true).execute(ctx);

        assertNull(result.differentialResult());
    }

    @Test
    void execute_conModuloDeshabilitado_deberiaOmitirElMotor() {
        var result = stage(false).execute(context());

        assertNull(result.differentialResult());
    }
}


