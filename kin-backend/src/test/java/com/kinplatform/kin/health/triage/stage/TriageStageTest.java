package com.kinplatform.kin.health.triage.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.kin.health.triage.event.TriagePerformedEvent;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.common.pipeline.PipelineContext;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TriageStageTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");
    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010001");

    private static TriageKnowledgeRepository repo() {
        var fiebre = Symptom.of(S1, "fiebre", "Temperatura elevada", "R50.9");
        var tos = Symptom.of(S2, "tos", "Tos", null);
        var gripe = Condition.of(
                C1, "Gripe", "Infección viral", "J11", Severity.MODERADO, Urgency.MEDIA, "Consulta médica.");
        return new InMemoryTriageKnowledgeRepository(new TriageCatalog(
                List.of(fiebre, tos),
                List.of(gripe),
                List.of(
                        SymptomConditionRelation.of(S1, C1, 0.9, true),
                        SymptomConditionRelation.of(S2, C1, 0.6, false))));
    }

    private static TriageStage stage(boolean enabled) {
        return new TriageStage(new TriageEngine(repo()), repo(), enabled);
    }

    private static ProjectContext projectContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.PROJECT_NAME, "Mi salud");
        return ProjectContext.restore(data, EnumSet.of(AnalyzedDimension.PROJECT_NAME), null, 1, false);
    }

    private static PipelineContext context(String message) {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, message, List.of(), "Mi salud", "Cuidado de salud", "SALUD");
        ctx.projectContext(projectContext());
        return ctx;
    }

    @Test
    void nombre_deberiaSerTriaje() {
        assertEquals("Triaje", stage(true).name());
    }

    @Test
    void supports_deberiaSerTrue_conContextoYModuloHabilitado() {
        assertTrue(stage(true).supports(context("tengo fiebre y tos")));
    }

    @Test
    void supports_deberiaSerFalse_cuandoElModuloEstaDeshabilitado() {
        assertFalse(stage(false).supports(context("tengo fiebre")));
    }

    @Test
    void supports_deberiaSerFalse_sinMensaje() {
        assertFalse(stage(true).supports(context("   ")));
    }

    @Test
    void supports_deberiaSerFalse_sinProjectContext() {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "tengo fiebre", List.of(), "T", "D", "C");
        assertFalse(stage(true).supports(ctx));
    }

    @Test
    void execute_conSintomas_deberiaEjecutarElMotorYAlmacenarElResultado() {
        var result = stage(true).execute(context("tengo fiebre y tos"));

        assertNotNull(result.triageResult());
        assertFalse(result.triageResult().isEmpty());
        assertEquals("Gripe", result.triageResult().results().get(0).name());
        assertTrue(result.engineResults().containsKey(TriageEngine.GENERATOR_NAME));
    }

    @Test
    void execute_conSintomas_deberiaEmitirTriagePerformedEvent() {
        var result = stage(true).execute(context("tengo fiebre"));

        boolean hasEvent = result.events().stream().anyMatch(e -> e instanceof TriagePerformedEvent);
        assertTrue(hasEvent);
    }

    @Test
    void execute_sinSintomasReconocidos_deberiaOmitirElMotor() {
        var result = stage(true).execute(context("cuéntame sobre mi proyecto"));

        assertNull(result.triageResult());
    }

    @Test
    void execute_conModuloDeshabilitado_deberiaOmitirElMotor() {
        var result = stage(false).execute(context("tengo fiebre"));

        assertNull(result.triageResult());
    }
}


