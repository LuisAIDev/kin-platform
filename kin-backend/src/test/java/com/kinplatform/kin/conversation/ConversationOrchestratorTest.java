package com.kinplatform.kin.conversation;

import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.KinMethodCommand;
import com.kinplatform.kin.KinMethodResult;
import com.kinplatform.kin.StreamingMethodOutcome;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.Message;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.policy.TurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private KinMethod kinMethod;

    @Mock
    private ContextRepository contextRepository;

    private final HistoryWindow historyWindow = new HistoryWindow();
    private final TurnPolicy turnPolicy = new DefaultTurnPolicy();
    private final ResponseGuard responseGuard = new ResponseGuard();

    private ConversationOrchestrator orchestrator() {
        return new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod, responseGuard, contextRepository);
    }

    private ProjectContext contextoExploracion() {
        return ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
    }

    private ProjectContext contextoReporte() {
        var contexto = ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
        contexto.markReportGenerated();
        contexto.attachDecision(ConversationDecision.generateReport("informe"));
        return contexto;
    }

    private void stubContexto(ProjectContext ctx) {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(ctx);
    }

    private void stubContextoExploracion() {
        stubContexto(contextoExploracion());
    }

    private ConversationTurn turn(List<Message> history) {
        return new ConversationTurn(PROJECT_ID, USER_ID,
            "¿En qué mercado vas a operar?", history,
            "Proyecto Test", "Descripción", "Software");
    }

    private KinMethodResult resultadoAsking(String response) {
        return new KinMethodResult(contextoExploracion(), null,
            ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta"),
            response, null,
            List.of(new QuestionGeneratedEvent(PROJECT_ID, "MARKET", "razón")), null);
    }

    private void stubEjecucion(KinMethodResult result) {
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(result);
    }

    private void stubEjecucionStream(Flux<String> flux) {
        when(kinMethod.executeStream(any(KinMethodCommand.class))).thenReturn(flux);
    }

    private List<Message> historial(int size) {
        List<Message> history = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            history.add(i % 2 == 0 ? Message.user("usuario-" + i) : Message.assistant("asistente-" + i));
        }
        return history;
    }

    @Test
    void flujoConversacion_exitoso_deberiaConstruirTurnResultCompleto() {
        var contexto = contextoExploracion();
        var decision = ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta");
        contexto.attachDecision(decision);
        var evento = new QuestionGeneratedEvent(PROJECT_ID, "MARKET", "razón");
        stubContexto(contexto);
        stubEjecucion(new KinMethodResult(contexto, null, decision,
            "¿Apuntás a consumidores o a empresas?", null, List.of(evento), null));

        var turn = turn(List.of());
        var result = orchestrator().orchestrate(turn);

        assertEquals(ConversationPhase.EXPLORATION, result.directive().phase());
        assertEquals(ConversationDecision.Action.ASK, result.directive().action());
        assertEquals(AnalyzedDimension.TARGET_CUSTOMER, result.directive().dimension());
        assertEquals(CommunicationMode.QUESTION, result.directive().communicationMode());
        assertEquals(TurnConstraints.question(), result.directive().constraints());

        assertSame(contexto, result.projectContext());
        assertSame(decision, result.decision());
        assertEquals("¿Apuntás a consumidores o a empresas?", result.aiResponse());
        assertTrue(result.validation().accepted());
        assertNull(result.consultingReport());
        assertEquals(1, result.events().size());
        assertSame(evento, result.events().get(0));

        verify(kinMethod).execute(any(KinMethodCommand.class));
    }

    @Test
    void flujoReporte_exitoso_deberiaPropagarConsultingReportYDirectivaReporting() {
        var contexto = contextoReporte();
        var decision = ConversationDecision.generateReport("informe");
        var reporte = ConsultingReport.empty();
        stubContexto(contexto);
        stubEjecucion(new KinMethodResult(contexto, null, decision,
            "Aquí tenés el informe de viabilidad completo.", null,
            List.of(new QuestionGeneratedEvent(PROJECT_ID, "MARKET", "razón")), reporte));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertEquals(ConversationPhase.REPORTING, result.directive().phase());
        assertEquals(ConversationDecision.Action.REPORT, result.directive().action());
        assertEquals(CommunicationMode.EXPLAIN_REPORT, result.directive().communicationMode());
        assertEquals(TurnConstraints.reportExplanation(), result.directive().constraints());
        assertSame(reporte, result.consultingReport());
        assertTrue(result.validation().accepted());
    }

    @Test
    void historyWindow_deberiaAcotarElHistorialAntesDeDelegar() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("¿Pregunta?"));
        var history = historial(30);

        orchestrator().orchestrate(turn(history));

        var captor = ArgumentCaptor.forClass(KinMethodCommand.class);
        verify(kinMethod).execute(captor.capture());
        assertEquals(HistoryWindow.DEFAULT_MAX_MESSAGES, captor.getValue().history().size());
        assertEquals(history.subList(10, 30), captor.getValue().history());
        assertSame(history.get(29), captor.getValue().history().get(19));
    }

    @Test
    void historyWindow_conHistorialMenorAlLimite_deberiaDelegarElHistorialCompleto() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("¿Pregunta?"));
        var history = historial(5);

        orchestrator().orchestrate(turn(history));

        var captor = ArgumentCaptor.forClass(KinMethodCommand.class);
        verify(kinMethod).execute(captor.capture());
        assertEquals(history, captor.getValue().history());
    }

    @Test
    void kinMethod_deberiaRecibirTodosLosCamposDelTurno() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("¿Pregunta?"));
        var history = historial(3);

        orchestrator().orchestrate(turn(history));

        var captor = ArgumentCaptor.forClass(KinMethodCommand.class);
        verify(kinMethod).execute(captor.capture());
        var command = captor.getValue();
        assertEquals(PROJECT_ID, command.projectId());
        assertEquals(USER_ID, command.userId());
        assertEquals("¿En qué mercado vas a operar?", command.userMessage());
        assertEquals("Proyecto Test", command.projectTitle());
        assertEquals("Descripción", command.projectDescription());
        assertEquals("Software", command.projectCategory());
    }

    @Test
    void directiva_deberiaViajarEnElComandoYEnmarcarElTurno() {
        var contexto = contextoExploracion();
        contexto.attachDecision(ConversationDecision.ask(AnalyzedDimension.PROBLEM, 10, "explorar"));
        stubContexto(contexto);
        stubEjecucion(resultadoAsking("¿Pregunta?"));

        orchestrator().orchestrate(turn(List.of()));

        var captor = ArgumentCaptor.forClass(KinMethodCommand.class);
        verify(kinMethod).execute(captor.capture());
        var directive = captor.getValue().directive();
        assertEquals(ConversationPhase.EXPLORATION, directive.phase());
        assertEquals(ConversationDecision.Action.ASK, directive.action());
        assertEquals(AnalyzedDimension.PROBLEM, directive.dimension());
        assertEquals(CommunicationMode.QUESTION, directive.communicationMode());
        assertEquals(TurnConstraints.question(), directive.constraints());
    }

    @Test
    void directivaDePolitica_deberiaPropagarseAlGuardYAlTurnResult() {
        var contexto = contextoExploracion();
        var decision = ConversationDecision.generateReport("informe");
        contexto.attachDecision(decision);
        var directiva = new TurnDirective(
            ConversationPhase.REPORTING, ConversationDecision.Action.REPORT, null,
            CommunicationMode.EXPLAIN_REPORT, TurnConstraints.reportExplanation());
        var policy = new TurnPolicy() {
            @Override
            public TurnDirective decide(ProjectContext context, ConversationDecision previousDecision) {
                assertSame(contexto, context);
                assertSame(decision, previousDecision);
                return directiva;
            }
        };
        stubContexto(contexto);
        stubEjecucion(new KinMethodResult(contexto, null, decision,
            "Explicación del informe.", null, List.of(), ConsultingReport.empty()));
        var orchestrator = new ConversationOrchestrator(historyWindow, policy, kinMethod, responseGuard, contextRepository);

        var result = orchestrator.orchestrate(turn(List.of()));

        assertSame(directiva, result.directive());
        assertTrue(result.validation().accepted());
    }

    @Test
    void responseGuard_deberiaRechazarRespuestaConMultiplesPreguntas() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("¿Apuntás a empresas? ¿O a consumidores?"));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertFalse(result.validation().accepted());
        assertTrue(result.validation().issues().contains("response.multiple_questions"));
    }

    @Test
    void responseGuard_deberiaRechazarRespuestaExcesivamenteLarga() {
        stubContextoExploracion();
        String respuestaLarga = "a".repeat(TurnConstraints.QUESTION_MAX_LENGTH + 1) + "?";
        stubEjecucion(resultadoAsking(respuestaLarga));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertFalse(result.validation().accepted());
        assertTrue(result.validation().issues().contains("response.too_long"));
        assertEquals(respuestaLarga, result.aiResponse());
    }

    @Test
    void responseGuard_deberiaRechazarMarcadoresProhibidosEnExploracion() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("Resumen: === CONSULTING REPORT ==="));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertFalse(result.validation().accepted());
        assertTrue(result.validation().issues().contains("response.forbidden_marker"));
    }

    @Test
    void responseGuard_deberiaRechazarRespuestaVacia() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking(null));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertFalse(result.validation().accepted());
        assertTrue(result.validation().issues().contains("response.empty"));
    }

    @Test
    void responseGuard_enFaseReporting_noDeberiaRechazarMarcadores() {
        var contexto = contextoReporte();
        var decision = ConversationDecision.generateReport("informe");
        stubContexto(contexto);
        stubEjecucion(new KinMethodResult(contexto, null, decision,
            "=== CONSULTING REPORT === resumen ejecutivo", null, List.of(),
            ConsultingReport.empty()));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertTrue(result.validation().accepted());
    }

    @Test
    void eventos_deberianPropagarseSinModificacion() {
        stubContextoExploracion();
        var evento = new QuestionGeneratedEvent(PROJECT_ID, "MARKET", "razón");
        stubEjecucion(new KinMethodResult(contextoExploracion(), null,
            ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta"),
            "¿Pregunta?", null, List.of(evento), null));

        var result = orchestrator().orchestrate(turn(List.of()));

        assertEquals(1, result.events().size());
        assertSame(evento, result.events().get(0));
    }

    @Test
    void turnNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> orchestrator().orchestrate(null));
        verify(kinMethod, never()).execute(any(KinMethodCommand.class));
    }

    @Test
    void resultadoNuloDeKinMethod_deberiaLanzar() {
        stubContextoExploracion();
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(null);

        assertThrows(IllegalStateException.class,
            () -> orchestrator().orchestrate(turn(List.of())));
    }

    @Test
    void decisionNulaEnResultado_deberiaLanzar() {
        stubContextoExploracion();
        stubEjecucion(new KinMethodResult(contextoExploracion(), null, null,
            "respuesta", null, List.of(), null));

        assertThrows(IllegalArgumentException.class,
            () -> orchestrator().orchestrate(turn(List.of())));
    }

    @Test
    void contextoNuloEnResultado_deberiaLanzar() {
        stubContextoExploracion();
        stubEjecucion(new KinMethodResult(null, null,
            ConversationDecision.ask(AnalyzedDimension.TARGET_CUSTOMER, 7, "pregunta"),
            "respuesta", null, List.of(), null));

        assertThrows(IllegalArgumentException.class,
            () -> orchestrator().orchestrate(turn(List.of())));
    }

    @Test
    void contextoNuloDelRepositorio_deberiaLanzar() {
        when(contextRepository.findOrCreate(eq(PROJECT_ID), eq("Proyecto Test"),
            eq("Descripción"), eq("Software"))).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
            () -> orchestrator().orchestrate(turn(List.of())));
        verify(kinMethod, never()).execute(any(KinMethodCommand.class));
    }

    @Test
    void constructor_deberiaRechazarDependenciasNulas() {
        assertThrows(IllegalArgumentException.class,
            () -> new ConversationOrchestrator(null, turnPolicy, kinMethod, responseGuard, contextRepository));
        assertThrows(IllegalArgumentException.class,
            () -> new ConversationOrchestrator(historyWindow, null, kinMethod, responseGuard, contextRepository));
        assertThrows(IllegalArgumentException.class,
            () -> new ConversationOrchestrator(historyWindow, turnPolicy, null, responseGuard, contextRepository));
        assertThrows(IllegalArgumentException.class,
            () -> new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod, null, contextRepository));
        assertThrows(IllegalArgumentException.class,
            () -> new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod, responseGuard, null));
    }

    @Test
    void determinismo_deberiaProducirElMismoTurnResult() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("¿Apuntás a empresas?"));

        var primero = orchestrator().orchestrate(turn(historial(4)));
        var segundo = orchestrator().orchestrate(turn(historial(4)));

        assertEquals(primero, segundo);
        verify(kinMethod, times(2)).execute(any(KinMethodCommand.class));
    }

    @Test
    void historialNuloEnTurno_deberiaNormalizarseAHistorialVacio() {
        stubContextoExploracion();
        stubEjecucion(resultadoAsking("¿Pregunta?"));

        var result = orchestrator().orchestrate(turn(null));

        assertTrue(result.validation().accepted());
        var captor = ArgumentCaptor.forClass(KinMethodCommand.class);
        verify(kinMethod).execute(captor.capture());
        assertTrue(captor.getValue().history().isEmpty());
    }

    @Test
    void orchestrateStream_deberiaDelegarEnExecuteStreamConLaDirectivaYDevolverElFlux() {
        var contexto = contextoExploracion();
        contexto.attachDecision(ConversationDecision.ask(AnalyzedDimension.SOLUTION, 10, "explorar"));
        stubContexto(contexto);
        stubEjecucionStream(Flux.just("a", "b"));

        var flux = orchestrator().orchestrateStream(turn(historial(3)));

        StepVerifier.create(flux)
                .expectNext("a", "b")
                .verifyComplete();

        var captor = ArgumentCaptor.forClass(KinMethodCommand.class);
        verify(kinMethod).executeStream(captor.capture());
        var command = captor.getValue();
        assertEquals(PROJECT_ID, command.projectId());
        assertEquals(USER_ID, command.userId());
        assertEquals(3, command.history().size());
        var directive = command.directive();
        assertEquals(ConversationPhase.EXPLORATION, directive.phase());
        assertEquals(ConversationDecision.Action.ASK, directive.action());
        assertEquals(AnalyzedDimension.SOLUTION, directive.dimension());
        assertEquals(CommunicationMode.QUESTION, directive.communicationMode());
    }

    @Test
    void orchestrateStream_fluxNuloDeberiaLanzar() {
        stubContextoExploracion();
        when(kinMethod.executeStream(any(KinMethodCommand.class))).thenReturn(null);

        assertThrows(IllegalStateException.class,
            () -> orchestrator().orchestrateStream(turn(List.of())));
    }

    @Test
    void orchestrateStream_turnNuloDeberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> orchestrator().orchestrateStream(null));
        verify(kinMethod, never()).executeStream(any(KinMethodCommand.class));
    }

    @Test
    void orchestrateStreamWithOutcome_deberiaEntregarDecisionYReporte() {
        var contexto = contextoReporte();
        stubContexto(contexto);
        var report = ConsultingReport.empty();
        var result = new KinMethodResult(contexto, null,
            ConversationDecision.generateReport("informe"), null, null, List.of(), report);
        when(kinMethod.executeStreamWithOutcome(any(KinMethodCommand.class)))
            .thenReturn(new StreamingMethodOutcome(Flux.just("a"), result));

        var outcome = orchestrator().orchestrateStreamWithOutcome(turn(List.of()));

        assertNotNull(outcome);
        assertEquals(ConversationDecision.Action.REPORT, outcome.decision().action());
        assertSame(report, outcome.consultingReport());
        StepVerifier.create(outcome.flux())
            .expectNext("a")
            .verifyComplete();
    }

    @Test
    void orchestrateStreamWithOutcome_fluxNuloDeberiaDevolverNull() {
        stubContextoExploracion();
        when(kinMethod.executeStreamWithOutcome(any(KinMethodCommand.class)))
            .thenReturn(null);

        assertNull(orchestrator().orchestrateStreamWithOutcome(turn(List.of())));
    }
}
