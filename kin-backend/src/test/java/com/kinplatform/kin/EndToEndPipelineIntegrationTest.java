package com.kinplatform.kin;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.TurnResult;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enrichment.EnrichmentEngine;
import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.event.ReportGeneratedEvent;
import com.kinplatform.kin.event.RiskDetectedEvent;
import com.kinplatform.kin.event.ScoreCalculatedEvent;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.scoring.ScoringEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FASE 9 · E6 — integración end-to-end del flujo real:
 * ConversationOrchestrator → KinMethod → Pipeline (13 etapas reales) → Response.
 */
@ExtendWith(MockitoExtension.class)
class EndToEndPipelineIntegrationTest {

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
    void flujoOrquestador_deberiaEjecutarElPipelineRealYProducirTurnResultConsistente() {
        stubContextoCompleto();
        when(aiResponder.respond(any(AIRequest.class)))
            .thenReturn("Aquí tenés el informe de viabilidad completo.");

        TurnResult result = orchestrator().orchestrate(turn());

        assertNotNull(result.projectContext());
        assertNotNull(result.decision());
        assertNotNull(result.directive());
        assertEquals(ConversationDecision.Action.REPORT, result.decision().action());
        assertFalse(result.aiResponse().isBlank());
        assertNotNull(result.validation());
        assertTrue(result.validation().accepted());
        assertNotNull(result.consultingReport());
        assertEquals("ReportEngine", result.consultingReport().generatedBy());
        assertTrue(result.consultingReport().metadata().sectionsIncluded().contains("Sources"));
        assertFalse(result.consultingReport().sources().isEmpty());
        assertFalse(result.events().isEmpty());
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ReportGeneratedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ScoreCalculatedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof RiskDetectedEvent));
        assertTrue(result.events().stream().anyMatch(e -> e instanceof ConversationCompletedEvent));
        verify(contextRepository).save(any(UUID.class), any(ProjectContext.class));
    }

    @Test
    void flujoPipelineReal_deberiaEjecutarLas13EtapasYProducirTodosLosResultados() {
        when(aiResponder.respond(any(AIRequest.class)))
            .thenReturn("Aquí tenés el informe de viabilidad completo.");

        var order = new java.util.ArrayList<String>();
        var pipeline = TestIntegrationPipeline.realPipeline(aiResponder, PROJECT_ID, order);
        var ctx = TestIntegrationPipeline.pipelineContext(PROJECT_ID, USER_ID);

        PipelineContext result = pipeline.execute(ctx);

        assertEquals(List.of("Analizador", "Evaluador", "Estratega", "Entrevista",
            "Conocimiento", "Enriquecimiento", "Scoring", "Recomendaciones", "Riesgos",
            "Oportunidades", "Reporte", "Consultor", "Eventos"), order);

        assertNotNull(result.knowledgeResult());
        assertFalse(result.knowledgeResult().isEmpty());
        assertEquals(KnowledgeEngine.GENERATOR_NAME, result.knowledgeResult().generatedBy());

        assertNotNull(result.enrichmentResult());
        assertFalse(result.enrichmentResult().isEmpty());
        assertEquals(EnrichmentEngine.GENERATOR_NAME, result.enrichmentResult().generatedBy());

        assertNotNull(result.scoreResult());
        assertEquals(ScoringEngine.GENERATOR_NAME, result.scoreResult().generatedBy());
        assertTrue(result.scoreResult().totalScore() > 0);

        assertNotNull(result.recommendationResult());
        assertNotNull(result.riskResult());
        assertNotNull(result.opportunityResult());

        assertNotNull(result.consultingReport());
        assertFalse(result.consultingReport().isEmpty());
        assertFalse(result.consultingReport().sources().isEmpty());

        assertFalse(result.aiResponse().isBlank());

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        assertTrue(captor.getValue().systemPrompt().contains("=== CONSULTING REPORT ==="));

        assertTrue(result.completed());
        assertFalse(result.events().isEmpty());
    }
}
