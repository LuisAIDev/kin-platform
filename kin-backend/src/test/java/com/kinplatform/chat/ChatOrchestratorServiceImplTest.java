package com.kinplatform.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.guardrails.PromptGuardrail;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.conversation.StreamingTurnOutcome;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.conversation.TurnResult;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import com.kinplatform.project.Category;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.project.ProjectStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRole;
import java.io.IOException;
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

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorServiceImplTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ConversationOrchestrator conversationOrchestrator;

    @Mock
    private ReportRepository reportRepository;

    private ObjectMapper objectMapper;
    private ChatOrchestratorServiceImpl orchestrator;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String CONTENT = "Test message";

    private ChatRequest request;
    private Project project;
    private ChatMessageResponse userMsgResponse;
    private ChatMessageResponse assistantMsgResponse;

    @BeforeEach
    void setUp() {
        objectMapper = spy(new ObjectMapper());

        ExecutorService syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kin-test-sync-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });

        orchestrator = new ChatOrchestratorServiceImpl(
                chatService,
                projectRepository,
                objectMapper,
                conversationOrchestrator,
                new PromptGuardrail(),
                reportRepository,
                null,
                null,
                null,
                null,
                syncExecutor,
                true);

        request = new ChatRequest();
        request.setContent(CONTENT);

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
                .category(Category.builder()
                        .code("EMPRESARIAL")
                        .name("Empresarial")
                        .displayOrder(2)
                        .color("#0ea5e9")
                        .active(true)
                        .build())
                .status(ProjectStatus.DRAFT)
                .build();

        userMsgResponse = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .projectId(PROJECT_ID)
                .role(MessageRole.USER)
                .content(CONTENT)
                .tokensUsed(0)
                .build();

        assistantMsgResponse = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .projectId(PROJECT_ID)
                .role(MessageRole.ASSISTANT)
                .content("")
                .tokensUsed(42)
                .build();
    }

    private void stubCommonDependencies() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(java.util.Optional.of(project));
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(userMsgResponse)
                .thenReturn(assistantMsgResponse);
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID)).thenReturn(List.of());
    }

    private void stubStream(Flux<String> flux) {
        when(conversationOrchestrator.orchestrateStreamWithOutcome(any(ConversationTurn.class)))
                .thenReturn(new StreamingTurnOutcome(
                        flux, ConversationDecision.ask(AnalyzedDimension.SECTOR, 5, "pregunta"), null));
    }

    private TurnResult turnResultReporte(ConsultingReport report) {
        var ctx = ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
        var decision = ConversationDecision.generateReport("informe");
        var directive = new TurnDirective(
                ConversationPhase.REPORTING,
                ConversationDecision.Action.REPORT,
                AnalyzedDimension.SECTOR,
                CommunicationMode.EXPLAIN_REPORT,
                TurnConstraints.reportExplanation());
        return new TurnResult(ctx, decision, directive, "respuesta", ResponseValidation.ok(), report, List.of());
    }

    private TurnResult turnResultAsk() {
        var ctx = ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.SECTOR, 5, "pregunta");
        var directive = new TurnDirective(
                ConversationPhase.EXPLORATION,
                ConversationDecision.Action.ASK,
                AnalyzedDimension.SECTOR,
                CommunicationMode.QUESTION,
                TurnConstraints.question());
        return new TurnResult(ctx, decision, directive, "¿pregunta?", ResponseValidation.ok(), null, List.of());
    }

    private void stubOrchestrate(TurnResult result) {
        when(conversationOrchestrator.orchestrate(any(ConversationTurn.class))).thenReturn(result);
    }

    // ---------------------------------------------------------------
    // Test 1 — flujo exitoso con 2 tokens
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirTokensYDone_cuandoFlujoExitoso() throws Exception {
        stubCommonDependencies();
        stubStream(Flux.just("A", "B"));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, times(4)).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(mockEmitter, never()).completeWithError(any());

            // Verify content & order of each JSON payload sent to ObjectMapper
            ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, times(4)).writeValueAsString(jsonCaptor.capture());

            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> startedPayload = (Map<String, Object>) payloads.get(0);
            assertEquals("Procesando tu mensaje...", startedPayload.get("message"));

            @SuppressWarnings("unchecked")
            Map<String, Object> firstToken = (Map<String, Object>) payloads.get(1);
            assertEquals("A", firstToken.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> secondToken = (Map<String, Object>) payloads.get(2);
            assertEquals("B", secondToken.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> donePayload = (Map<String, Object>) payloads.get(3);
            assertEquals(true, donePayload.get("done"));
            assertEquals("AB", donePayload.get("content"));

            // Verify assistant message persisted with accumulated content
            ArgumentCaptor<SaveMessageRequest> saveCaptor = ArgumentCaptor.forClass(SaveMessageRequest.class);
            verify(chatService, times(2)).saveMessage(eq(USER_ID), eq(PROJECT_ID), saveCaptor.capture());

            var saveRequests = saveCaptor.getAllValues();
            assertEquals(MessageRole.USER, saveRequests.get(0).getRole());
            assertEquals(CONTENT, saveRequests.get(0).getContent());
            assertEquals(MessageRole.ASSISTANT, saveRequests.get(1).getRole());
            assertEquals("AB", saveRequests.get(1).getContent());
        }
    }

    // ---------------------------------------------------------------
    // Test 2 — error en el flux (onError)
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirError_cuandoFlujoFalla() throws Exception {
        stubCommonDependencies();
        stubStream(Flux.error(new RuntimeException("AI model unavailable")));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(mockEmitter, never()).completeWithError(any());

            // Verify error payload (first is started, second is error)
            ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, times(2)).writeValueAsString(jsonCaptor.capture());

            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> startedPayload = (Map<String, Object>) payloads.get(0);
            assertEquals("Procesando tu mensaje...", startedPayload.get("message"));

            @SuppressWarnings("unchecked")
            Map<String, Object> errorPayload = (Map<String, Object>) payloads.get(1);
            assertEquals("AI model unavailable", errorPayload.get("error"));

            // Assistant message should NOT be saved (error path)
            verify(chatService, times(1)).saveMessage(eq(USER_ID), eq(PROJECT_ID), any());
        }
    }

    // ---------------------------------------------------------------
    // Test 3 — error al guardar assistant message (onComplete falla): la
    // finalización fallida emite un único evento de error y completa, sin
    // enviar done (no se convierte un fallo de persistencia en éxito).
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirError_cuandoLaFinalizacionDelTurnoFalla() throws Exception {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(java.util.Optional.of(project));
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(userMsgResponse)
                .thenThrow(new RuntimeException("DB error"));
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID)).thenReturn(List.of());
        stubStream(Flux.just("Token \u00FAnico"));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(mockEmitter, never()).completeWithError(any());

            // Verify error payload (no done fallback)
            ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, times(3)).writeValueAsString(jsonCaptor.capture());

            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> startedPayload = (Map<String, Object>) payloads.get(0);
            assertEquals("Procesando tu mensaje...", startedPayload.get("message"));

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenPayload = (Map<String, Object>) payloads.get(1);
            assertEquals("Token \u00FAnico", tokenPayload.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> errorPayload = (Map<String, Object>) payloads.get(2);
            assertTrue(errorPayload.get("error").toString().contains("No se pudo finalizar la respuesta"));
            assertTrue(errorPayload.get("error").toString().contains("DB error"));
        }
    }

    // ---------------------------------------------------------------
    // Test 4 — IOException al enviar token SSE
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_cuandoSendLanzaIOException_marcaCompletadoSinCompleteWithError() throws Exception {
        stubCommonDependencies();
        // No stubStream needed - send throws immediately

        try (var mocked = mockConstruction(SseEmitter.class, (mock, context) -> doThrow(new IOException("Broken pipe"))
                .when(mock)
                .send(any(SseEmitter.SseEventBuilder.class)))) {

            SseEmitter result = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            // El nuevo comportamiento no ejecuta completeWithError (evita el
            // "ResponseBodyEmitter has already completed"): el emitter queda
            // marcado como completado y la suscripción se cancela.
            verify(mockEmitter, never()).completeWithError(any());
        }
    }

    // ---------------------------------------------------------------
    // Test 5 — stream vacío (Flux.empty)
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirDoneConContenidoVacio_cuandoFluxVacio() throws Exception {
        stubCommonDependencies();
        stubStream(Flux.empty());

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();

            // Verify done payload has empty content
            ArgumentCaptor<Object> jsonCaptor = ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, times(2)).writeValueAsString(jsonCaptor.capture());

            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> startedPayload = (Map<String, Object>) payloads.get(0);
            assertEquals("Procesando tu mensaje...", startedPayload.get("message"));

            @SuppressWarnings("unchecked")
            Map<String, Object> donePayload = (Map<String, Object>) payloads.get(1);
            assertEquals(true, donePayload.get("done"));
            assertEquals("", donePayload.get("content"));

            // Verify assistant saved with empty content
            ArgumentCaptor<SaveMessageRequest> saveCaptor = ArgumentCaptor.forClass(SaveMessageRequest.class);
            verify(chatService, times(2)).saveMessage(eq(USER_ID), eq(PROJECT_ID), saveCaptor.capture());

            var saveRequests = saveCaptor.getAllValues();
            assertEquals("", saveRequests.get(1).getContent());
        }
    }

    // ---------------------------------------------------------------
    // Test 6 — persistencia del ConsultingReport (decisión REPORT)
    // ---------------------------------------------------------------
    @Test
    void processMessage_deberiaPersistirReporte_cuandoDecideREPORT() {
        stubCommonDependencies();
        var report = ConsultingReport.empty();
        stubOrchestrate(turnResultReporte(report));

        var result = orchestrator.processMessage(USER_ID, PROJECT_ID, request);

        assertNotNull(result);
        verify(reportRepository).save(PROJECT_ID, report);
    }

    @Test
    void processMessage_noDebePersistirReporte_cuandoNoDecideREPORT() {
        stubCommonDependencies();
        stubOrchestrate(turnResultAsk());

        orchestrator.processMessage(USER_ID, PROJECT_ID, request);

        verify(reportRepository, never()).save(any(), any());
    }

    @Test
    void processMessageStream_deberiaPersistirReporte_cuandoOutcomeREPORT() throws IOException {
        stubCommonDependencies();
        var report = ConsultingReport.empty();
        when(conversationOrchestrator.orchestrateStreamWithOutcome(any(ConversationTurn.class)))
                .thenReturn(new StreamingTurnOutcome(
                        Flux.just("A"), ConversationDecision.generateReport("informe"), report));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);
            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);
            verify(mockEmitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
        }

        verify(reportRepository).save(PROJECT_ID, report);
    }
}


