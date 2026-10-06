package com.kinplatform.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.guardrails.PromptGuardrail;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.event.ConversationCompletedEvent;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.QuestionGeneratedEvent;
import com.kinplatform.kin.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRole;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * Regresión de la cadena REAL (sin mockear el orquestador):
 * ChatOrchestratorService → ConversationOrchestrator → KinMethod → eventos →
 * ChatTurnFinalizationService → OutboxEventPublisher.
 *
 * <p>Detecta el agujero de cobertura de la auditoría: un turno que produce
 * eventos NO debe terminar con "OutboxEventPublisher requiere una transacción
 * activa"; la publicación debe ocurrir en la finalización del turno (la
 * transacción real se cubre con el test de integración Postgres, BLOCKED sin
 * Docker).</p>
 */
@ExtendWith(MockitoExtension.class)
class ChatStreamingFinalizationRegressionTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String QUESTION = "¿Apuntás a consumidores o a empresas?";

    @Mock
    private ChatService chatService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    private ChatTurnFinalizationService finalizationService;
    private ObjectMapper objectMapper;
    private Project project;

    @BeforeEach
    void setUp() {
        objectMapper = spy(new ObjectMapper());
        finalizationService = new ChatTurnFinalizationService(chatService, reportRepository, outboxEventPublisher);

        var user = User.builder()
                .id(USER_ID)
                .email("u@t.com")
                .fullName("U")
                .role(UserRole.FREE)
                .build();
        project = Project.builder()
                .id(PROJECT_ID)
                .user(user)
                .title("Proyecto Test")
                .description("Descripción")
                .category(null)
                .build();
    }

    private ConversationOrchestrator realOrchestrator() {
        var kinMethod = MinimalKinPipeline.build(aiResponder, contextRepository);
        return new ConversationOrchestrator(
                new HistoryWindow(), new DefaultTurnPolicy(), kinMethod, new ResponseGuard(), contextRepository);
    }

    private ChatOrchestratorServiceImpl orchestratorService() {
        ExecutorService syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kin-test-chain-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        return new ChatOrchestratorServiceImpl(
                chatService,
                projectRepository,
                objectMapper,
                realOrchestrator(),
                new PromptGuardrail(),
                reportRepository,
                null,
                null,
                null,
                null,
                finalizationService,
                syncExecutor,
                true);
    }

    private void stubSaveMessage() {
        when(chatService.saveMessage(any(UUID.class), any(UUID.class), any(SaveMessageRequest.class)))
                .thenAnswer(inv -> {
                    SaveMessageRequest request = inv.getArgument(2);
                    return ChatMessageResponse.builder()
                            .id(UUID.randomUUID())
                            .userId(USER_ID)
                            .projectId(PROJECT_ID)
                            .role(request.getRole())
                            .content(request.getContent())
                            .tokensUsed(7)
                            .build();
                });
    }

    private void stubStreamContext() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(java.util.Optional.of(project));
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID)).thenReturn(List.of());
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", null))
                .thenReturn(ProjectContext.fromProject("Proyecto Test", "Descripción", "Software"));
        when(aiResponder.respondStream(any(AIRequest.class)))
                .thenReturn(Flux.just("¿Apuntás a ", "consumidores o a empresas?"));
    }

    @Test
    void processMessageStream_cadenaReal_publicaEventosEnOutboxSinErrorTransaccional() throws Exception {
        stubStreamContext();
        stubSaveMessage();

        ChatRequest request = new ChatRequest();
        request.setContent("¿a quién apuntás?");

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestratorService().processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);
            // started + 2 tokens + done
            verify(mockEmitter, org.mockito.Mockito.times(4)).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(mockEmitter, never()).completeWithError(any());
        }

        // Los eventos del pipeline llegaron al outbox vía la finalización
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outboxEventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        boolean hasConversationCompleted =
                eventCaptor.getAllValues().stream().anyMatch(e -> e instanceof ConversationCompletedEvent);
        boolean hasQuestion = eventCaptor.getAllValues().stream().anyMatch(e -> e instanceof QuestionGeneratedEvent);
        assertTrue(hasConversationCompleted, "debe publicarse ConversationCompletedEvent");
        assertTrue(hasQuestion, "debe publicarse QuestionGeneratedEvent");
    }

    @Test
    void processMessageStream_cadenaReal_emiteStartedTokensDoneYComplete() throws Exception {
        stubStreamContext();
        stubSaveMessage();

        ChatRequest request = new ChatRequest();
        request.setContent("¿a quién apuntás?");

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestratorService().processMessageStream(USER_ID, PROJECT_ID, request);
            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, org.mockito.Mockito.times(4)).writeValueAsString(jsonCaptor.capture());
            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> started = (Map<String, Object>) payloads.get(0);
            assertEquals("Procesando tu mensaje...", started.get("message"));

            @SuppressWarnings("unchecked")
            Map<String, Object> token1 = (Map<String, Object>) payloads.get(1);
            assertEquals("¿Apuntás a ", token1.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> token2 = (Map<String, Object>) payloads.get(2);
            assertEquals("consumidores o a empresas?", token2.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> done = (Map<String, Object>) payloads.get(3);
            assertEquals(Boolean.TRUE, done.get("done"));
            assertEquals(QUESTION, done.get("content"));
            assertNotNull(done.get("assistantMessageId"));
            assertEquals(7, done.get("tokensUsed"));

            verify(mockEmitter).complete();
        }
    }

    @Test
    void processMessage_cadenaReal_finalizaYPublicaEventos() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(java.util.Optional.of(project));
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID)).thenReturn(List.of());
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", null))
                .thenReturn(ProjectContext.fromProject("Proyecto Test", "Descripción", "Software"));
        when(aiResponder.respond(any(AIRequest.class))).thenReturn(QUESTION);
        stubSaveMessage();

        ChatRequest request = new ChatRequest();
        request.setContent("¿a quién apuntás?");

        ChatResponse response = orchestratorService().processMessage(USER_ID, PROJECT_ID, request);

        assertNotNull(response);
        assertEquals(QUESTION, response.getContent());
        assertNotNull(response.getAssistantMessageId());

        verify(outboxEventPublisher, atLeastOnce()).publish(any(DomainEvent.class));
        verify(reportRepository, never()).save(any(UUID.class), any());
    }

    /** Pipeline real mínimo que produce la decisión ASK y sus eventos. */
    private static final class MinimalKinPipeline {
        private MinimalKinPipeline() {}

        static com.kinplatform.kin.KinMethod build(AIResponder aiResponder, ContextRepository contextRepository) {
            var promptAssembler = new PromptAssembler(
                    new ConversationPromptBuilder(),
                    new ReportPromptBuilder(List.of(
                            new com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter(),
                            new com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter())));
            var pipeline = new Pipeline(List.of(
                    new com.kinplatform.kin.pipeline.stage.AnalyzerStage(
                            (message, ctx) -> com.kinplatform.common.context.AnalysisResult.empty()),
                    new com.kinplatform.kin.pipeline.stage.EvaluatorStage(
                            new com.kinplatform.common.context.CompletenessEvaluator(
                                    com.kinplatform.common.context.EvaluationPolicies.defaults())),
                    new com.kinplatform.kin.pipeline.stage.StrategistStage(
                            new com.kinplatform.common.context.strategy.ConversationStrategist(
                                    new com.kinplatform.common.context.strategy.DefaultExplorationStrategy(
                                            com.kinplatform.common.context.ExplorationPriority.defaultPriorities()))),
                    new com.kinplatform.kin.pipeline.stage.ScoringStage(new com.kinplatform.platform.scoring.ScoringEngine(
                            com.kinplatform.platform.scoring.ScoringModel.defaultModel())),
                    new com.kinplatform.kin.pipeline.stage.ConsultorStage(
                            aiResponder, promptAssembler, new ResponseGuard()),
                    new com.kinplatform.kin.pipeline.stage.EventStage()));
            return new com.kinplatform.kin.KinMethod(pipeline, contextRepository);
        }
    }
}




