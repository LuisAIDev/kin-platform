package com.kinplatform.kin.interview.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.interview.InterviewInput;
import com.kinplatform.kin.interview.InterviewQuestion;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.interview.InMemoryInterviewRepository;
import com.kinplatform.kin.interview.engine.AnswerValidator;
import com.kinplatform.kin.interview.engine.InterviewBlueprint;
import com.kinplatform.kin.interview.engine.InterviewEngine;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewStageTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private InterviewEngine engine;

    private InterviewBlueprint blueprint() {
        return new InterviewBlueprint(List.of(
            InterviewQuestion.required("q-sector", AnalyzedDimension.SECTOR, "sector del negocio", 1),
            InterviewQuestion.required("q-revenue", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", 2)));
    }

    private InterviewEngine realEngine() {
        return new InterviewEngine(blueprint(), new AnswerValidator());
    }

    private ProjectContext projectContext() {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        data.put(AnalyzedDimension.PROJECT_NAME, "Mi negocio");
        return ProjectContext.restore(data, EnumSet.of(AnalyzedDimension.PROJECT_NAME), null, 1, false);
    }

    private PipelineContext context() {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "mensaje del turno", List.of(),
            "Proyecto Test", "Descripción", "Software");
        ctx.projectContext(projectContext());
        return ctx;
    }

    private void stubEngine(InterviewResult result) {
        when(engine.metadata()).thenReturn(EngineMetadata.of(InterviewEngine.GENERATOR_NAME,
            InterviewEngine.ENGINE_VERSION, "KIN", EnginePhase.VALIDATION, EngineType.DOMAIN, 40));
        when(engine.evaluate(any(InterviewInput.class))).thenReturn(result);
    }

    @Test
    void nombre_deberiaSerEntrevista() {
        assertEquals("Entrevista", new InterviewStage(realEngine()).name());
    }

    @Test
    void supports_deberiaSerTrue_conProjectContext() {
        assertTrue(new InterviewStage(realEngine()).supports(context()));
    }

    @Test
    void supports_deberiaSerFalse_sinProjectContext() {
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "hola", List.of(), "T", "D", "C");
        assertFalse(new InterviewStage(realEngine()).supports(ctx));
    }

    @Test
    void supports_deberiaSerFalse_conContextoNulo() {
        assertFalse(new InterviewStage(realEngine()).supports(null));
    }

    @Test
    void execute_deberiaEjecutarElMotor() {
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine);

        stage.execute(context());

        verify(engine).evaluate(any(InterviewInput.class));
    }

    @Test
    void execute_deberiaConstruirInterviewInputCorrecto() {
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine);

        stage.execute(context());

        ArgumentCaptor<InterviewInput> captor = ArgumentCaptor.forClass(InterviewInput.class);
        verify(engine).evaluate(captor.capture());
        var input = captor.getValue();
        assertEquals(PROJECT_ID, input.request().context().projectId());
        assertEquals("Proyecto Test", input.request().context().projectTitle());
        assertEquals("Software", input.request().context().projectCategory());
        assertEquals(Set.of(AnalyzedDimension.PROJECT_NAME), input.request().context().coveredDimensions());
        assertEquals("mensaje del turno", input.userMessage());
        assertNull(input.request().answer());
        assertEquals(PROJECT_ID, input.request().previousState().projectId());
        assertFalse(input.request().previousState().isComplete());
    }

    @Test
    void execute_deberiaAlmacenarInterviewResultEnPipelineContext() {
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine);

        var result = stage.execute(context());

        assertNotNull(result.interviewResult());
        assertTrue(result.interviewResult().isEmpty());
    }

    @Test
    void execute_deberiaAlmacenarResultadoDeMotorReal() {
        var stage = new InterviewStage(realEngine());

        var result = stage.execute(context());

        assertNotNull(result.interviewResult());
        assertTrue(result.interviewResult().hasDirective());
        assertEquals("q-sector", result.interviewResult().directive().questionId());
        assertTrue(result.interviewResult().decision().isAsk());
    }

    @Test
    void execute_deberiaRegistrarEnEngineResults() {
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine);

        var result = stage.execute(context());

        assertTrue(result.engineResults().containsKey(InterviewEngine.GENERATOR_NAME));
        assertNotNull(result.engineResult(InterviewEngine.GENERATOR_NAME));
    }

    @Test
    void execute_deberiaRegistrarEnEngineResults_conMotorReal() {
        var result = new InterviewStage(realEngine()).execute(context());

        assertTrue(result.engineResults().containsKey(InterviewEngine.GENERATOR_NAME));
        assertEquals(InterviewEngine.GENERATOR_NAME, result.interviewResult().generatedBy());
    }

    @Test
    void execute_deberiaRetornarElMismoContexto() {
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine);

        var original = context();
        var result = stage.execute(original);

        assertEquals(original, result);
        assertEquals(original.interviewResult(), result.interviewResult());
    }

    @Test
    void execute_conProyectoSinDatosCubiertos_deberiaMantenerLaProyeccionVacia() {
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine);
        var ctx = new PipelineContext(PROJECT_ID, USER_ID, "hola", List.of(), "T", "D", "C");
        ctx.projectContext(ProjectContext.fromProject("", "", ""));

        stage.execute(ctx);

        ArgumentCaptor<InterviewInput> captor = ArgumentCaptor.forClass(InterviewInput.class);
        verify(engine).evaluate(captor.capture());
        assertTrue(captor.getValue().request().context().coveredDimensions().isEmpty());
    }

    @Test
    void execute_deberiaPersistirElEstadoEntreTurnos() {
        var repo = new InMemoryInterviewRepository();
        var stage = new InterviewStage(realEngine(), repo);

        stage.execute(context());

        var persisted = repo.find(PROJECT_ID).orElseThrow();
        assertEquals("q-sector", persisted.current());
        assertTrue(persisted.pendingCount() > 0);
        assertEquals(1, persisted.exchangeUsed());
    }

    @Test
    void execute_deberiaConstruirLaRespuestaDelTurnoDesdeElEstadoPrevio() {
        var repo = new InMemoryInterviewRepository(
            InterviewState.empty(PROJECT_ID).withCurrent("q-sector"));
        var stage = new InterviewStage(realEngine(), repo);

        stage.execute(context());

        var persisted = repo.find(PROJECT_ID).orElseThrow();
        assertTrue(persisted.hasAnswered("q-sector"));
    }

    @Test
    void execute_conEntrevistaActiva_deberiaGobernarLaDecisionAsk() {
        var repo = new InMemoryInterviewRepository();
        var stage = new InterviewStage(realEngine(), repo);
        var ctx = context();

        var result = stage.execute(ctx);

        assertEquals(ConversationDecision.Action.ASK, result.decision().action());
        assertEquals(AnalyzedDimension.SECTOR, result.decision().dimension());
        assertSame(result.decision(), result.projectContext().currentDecision());
    }

    @Test
    void execute_conEntrevistaCompleta_deberiaHabilitarElReporte() {
        var repo = new InMemoryInterviewRepository(
            InterviewState.empty(PROJECT_ID).withComplete(true));
        var stage = new InterviewStage(realEngine(), repo);
        var ctx = context();

        var result = stage.execute(ctx);

        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertTrue(result.projectContext().reportGenerated());
        assertTrue(result.interviewResult().complete());
    }

    @Test
    void execute_conResultadoVacio_deberiaNoModificarLaDecisionNiPersistir() {
        var repo = new InMemoryInterviewRepository();
        stubEngine(InterviewResult.empty());
        var stage = new InterviewStage(engine, repo);
        var ctx = context();
        ctx.decision(ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar"));

        var result = stage.execute(ctx);

        assertEquals(ConversationDecision.Action.ASK, result.decision().action());
        assertSame(ctx.decision(), result.decision());
        assertTrue(repo.find(PROJECT_ID).isEmpty());
    }

    @Test
    void constructor_deberiaRechazarRepositorioNulo() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewStage(realEngine(), null));
    }
}
