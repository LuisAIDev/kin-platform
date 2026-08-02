package com.kinplatform.kin;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.ResponseFallback;
import com.kinplatform.kin.conversation.TurnResult;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
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
import static org.mockito.Mockito.when;

/**
 * FASE 9 · E6 — flujo integrado con consumo real de {@code ResponseValidation}
 * y {@link ResponseFallback} (ADR-017 E5) dentro del pipeline real.
 */
@ExtendWith(MockitoExtension.class)
class PipelineFlowIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    private ConversationOrchestrator orchestrator() {
        var kinMethod = TestIntegrationPipeline.realKinMethod(aiResponder, contextRepository,
            new InMemoryDomainEventBus(), PROJECT_ID);
        return new ConversationOrchestrator(new HistoryWindow(), new DefaultTurnPolicy(),
            kinMethod, new ResponseGuard(), contextRepository);
    }

    private ConversationTurn turn() {
        return new ConversationTurn(PROJECT_ID, USER_ID, "generá el informe", List.of(),
            "Proyecto Test", "Descripción", "Software");
    }

    private void stubContextoCompleto() {
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(TestIntegrationPipeline.fullContext());
    }

    @Test
    void flujoAceptado_deberiaConsumirLaValidacion() {
        stubContextoCompleto();
        when(aiResponder.respond(any(AIRequest.class)))
            .thenReturn("Aquí tenés el informe de viabilidad completo.");

        TurnResult result = orchestrator().orchestrate(turn());

        assertNotNull(result.validation());
        assertTrue(result.validation().accepted());
        assertEquals("Aquí tenés el informe de viabilidad completo.", result.aiResponse());
        assertNotNull(result.consultingReport());
    }

    @Test
    void flujoRechazado_deberiaAplicarFallbackSinExcepcion() {
        stubContextoCompleto();
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("¿A? ¿B?");

        TurnResult result = orchestrator().orchestrate(turn());

        assertNotNull(result.validation());
        assertFalse(result.validation().accepted());
        assertTrue(result.validation().issues().contains("response.multiple_questions"));
        assertEquals(ResponseFallback.DEFAULT_CANNED_RESPONSE + " Motivo: response.multiple_questions.",
            result.aiResponse());
        assertNotNull(result.aiResponse());
        assertNotNull(result.consultingReport());
        assertFalse(result.events().isEmpty());
    }
}
