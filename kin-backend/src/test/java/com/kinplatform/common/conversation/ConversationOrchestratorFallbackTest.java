package com.kinplatform.common.conversation;

import com.kinplatform.common.method.KinMethod;
import com.kinplatform.common.method.KinMethodCommand;
import com.kinplatform.common.method.KinMethodResult;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.conversation.history.HistoryWindow;
import com.kinplatform.common.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.common.conversation.policy.TurnPolicy;
import com.kinplatform.common.conversation.validation.ResponseGuard;
import com.kinplatform.common.decision.ConversationDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorFallbackTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private KinMethod kinMethod;

    @Mock
    private ContextRepository contextRepository;

    private final HistoryWindow historyWindow = new HistoryWindow();
    private final TurnPolicy turnPolicy = new DefaultTurnPolicy();
    private final ResponseGuard responseGuard = new ResponseGuard();

    private ConversationOrchestrator orchestrator(ResponseFallback fallback) {
        return new ConversationOrchestrator(historyWindow, turnPolicy, kinMethod,
            responseGuard, contextRepository, fallback);
    }

    private ProjectContext contexto() {
        var ctx = ProjectContext.fromProject("Proyecto", "Desc", "Software");
        ctx.attachDecision(ConversationDecision.ask(AnalyzedDimension.SECTOR, 5, "pregunta"));
        return ctx;
    }

    private KinMethodResult resultado(String response) {
        return new KinMethodResult(contexto(), null,
            ConversationDecision.ask(AnalyzedDimension.SECTOR, 5, "pregunta"),
            response, null, List.of(), null);
    }

    private ConversationTurn turn() {
        return new ConversationTurn(PROJECT_ID, USER_ID, "hola", List.of(),
            "Proyecto", "Desc", "Software");
    }

    private void stubContexto() {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto", "Desc", "Software"))
            .thenReturn(contexto());
    }

    @Test
    void validacionAceptada_deberiaConservarLaRespuestaSinReintento() {
        stubContexto();
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(resultado("¿Pregunta?"));

        var result = orchestrator(new ResponseFallback(List.of("segura"), 2)).orchestrate(turn());

        assertTrue(result.validation().accepted());
        assertEquals("¿Pregunta?", result.aiResponse());
        verify(kinMethod, times(1)).execute(any(KinMethodCommand.class));
    }

    @Test
    void validacionRechazada_sinRetry_deberiaDevolverRespuestaSegura() {
        stubContexto();
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(resultado("¿A? ¿B?"));

        var result = orchestrator(new ResponseFallback(List.of("respuesta segura"), 0)).orchestrate(turn());

        assertFalse(result.validation().accepted());
        assertEquals("respuesta segura",
            result.aiResponse());
        assertNotNull(result.aiResponse());
        verify(kinMethod, times(1)).execute(any(KinMethodCommand.class));
    }

    @Test
    void rechazoPorLongitud_deberiaEntregarElContenidoUtilSinFallback() {
        stubContexto();
        String base = "## Dónde podría quedar bien tu restaurante\n"
            + "- Calle del Arsenal\n- Plaza Santo Domingo\n- Callejón Ancho\n- Calle de la Mantilla\n"
            + "Estas zonas tienen alto tránsito peatonal y turístico, ideales para un restaurante.\n";
        String respuestaLarga = base + "a".repeat(TurnConstraints.QUESTION_MAX_LENGTH);
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(resultado(respuestaLarga));

        var result = orchestrator(new ResponseFallback(List.of("respuesta segura"), 0)).orchestrate(turn());

        assertFalse(result.validation().accepted());
        assertTrue(result.validation().issues().contains("response.too_long"));
        assertEquals(respuestaLarga, result.aiResponse());
        assertFalse(result.aiResponse().contains("respuesta segura"));
        verify(kinMethod, times(1)).execute(any(KinMethodCommand.class));
    }

    @Test
    void validacionRechazada_conRetry_deberiaReintentarYUsarLaRespuestaValida() {
        stubContexto();
        when(kinMethod.execute(any(KinMethodCommand.class)))
            .thenReturn(resultado("¿A? ¿B?"))
            .thenReturn(resultado("¿Pregunta válida?"));

        var result = orchestrator(new ResponseFallback(List.of("segura"), 2)).orchestrate(turn());

        assertTrue(result.validation().accepted());
        assertEquals("¿Pregunta válida?", result.aiResponse());
        verify(kinMethod, times(2)).execute(any(KinMethodCommand.class));
    }

    @Test
    void validacionRechazada_conRetryAgotado_deberiaDevolverRespuestaSeguraSinLanzar() {
        stubContexto();
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(resultado("¿A? ¿B?"));

        var result = orchestrator(new ResponseFallback(List.of("segura"), 2)).orchestrate(turn());

        assertFalse(result.validation().accepted());
        assertEquals("segura",
            result.aiResponse());
        assertNotNull(result.aiResponse());
        verify(kinMethod, times(3)).execute(any(KinMethodCommand.class));
    }

    @Test
    void validacionRechazada_deberiaAcumularEventosDeLosReintentos() {
        stubContexto();
        when(kinMethod.execute(any(KinMethodCommand.class)))
            .thenReturn(resultado("¿A? ¿B?"))
            .thenReturn(resultado("¿Pregunta válida?"));

        var result = orchestrator(new ResponseFallback(List.of("segura"), 2)).orchestrate(turn());

        assertTrue(result.validation().accepted());
        assertEquals(0, result.events().size());
        verify(kinMethod, times(2)).execute(any(KinMethodCommand.class));
    }
}





