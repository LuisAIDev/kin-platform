package com.kinplatform.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.AiEngineService;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectCategory;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.project.ProjectStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorServiceImplTest {

    @Mock
    private ChatService chatService;

    @Mock
    private AiEngineService aiEngineService;

    @Mock
    private ProjectRepository projectRepository;

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

        orchestrator = new ChatOrchestratorServiceImpl(
                chatService, aiEngineService, projectRepository, objectMapper);

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
                .category(ProjectCategory.EMPRENDIMIENTO)
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
        when(projectRepository.findById(PROJECT_ID))
                .thenReturn(java.util.Optional.of(project));
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(userMsgResponse)
                .thenReturn(assistantMsgResponse);
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID))
                .thenReturn(List.of());
    }

    // ---------------------------------------------------------------
    // Test 1 — flujo exitoso con 2 tokens
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirTokensYDone_cuandoFlujoExitoso() throws Exception {
        stubCommonDependencies();
        when(aiEngineService.generateAiResponseStream(
                anyList(), eq(CONTENT), anyString(), anyString(), anyString()))
                .thenReturn(Flux.just("A", "B"));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator
                    .processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, times(3))
                    .send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(mockEmitter, never()).completeWithError(any());

            // Verify content & order of each JSON payload sent to ObjectMapper
            ArgumentCaptor<Object> jsonCaptor =
                    ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, times(3))
                    .writeValueAsString(jsonCaptor.capture());

            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> firstToken =
                    (Map<String, Object>) payloads.get(0);
            assertEquals("A", firstToken.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> secondToken =
                    (Map<String, Object>) payloads.get(1);
            assertEquals("B", secondToken.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> donePayload =
                    (Map<String, Object>) payloads.get(2);
            assertEquals(true, donePayload.get("done"));
            assertEquals("AB", donePayload.get("content"));

            // Verify assistant message persisted with accumulated content
            ArgumentCaptor<SaveMessageRequest> saveCaptor =
                    ArgumentCaptor.forClass(SaveMessageRequest.class);
            verify(chatService, times(2))
                    .saveMessage(eq(USER_ID), eq(PROJECT_ID),
                            saveCaptor.capture());

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
        when(aiEngineService.generateAiResponseStream(
                anyList(), eq(CONTENT), anyString(), anyString(), anyString()))
                .thenReturn(Flux.error(new RuntimeException("AI model unavailable")));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator
                    .processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(mockEmitter, never()).completeWithError(any());

            // Verify error payload
            ArgumentCaptor<Object> jsonCaptor =
                    ArgumentCaptor.forClass(Object.class);
            verify(objectMapper).writeValueAsString(jsonCaptor.capture());

            @SuppressWarnings("unchecked")
            Map<String, Object> errorPayload =
                    (Map<String, Object>) jsonCaptor.getValue();
            assertEquals("AI model unavailable", errorPayload.get("error"));

            // Assistant message should NOT be saved (error path)
            verify(chatService, times(1))
                    .saveMessage(eq(USER_ID), eq(PROJECT_ID), any());
        }
    }

    // ---------------------------------------------------------------
    // Test 3 — error al guardar assistant message (onComplete falla)
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirDoneFallback_cuandoSaveAssistantFalla() throws Exception {
        when(projectRepository.findById(PROJECT_ID))
                .thenReturn(java.util.Optional.of(project));
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(userMsgResponse)
                .thenThrow(new RuntimeException("DB error"));
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID))
                .thenReturn(List.of());
        when(aiEngineService.generateAiResponseStream(
                anyList(), eq(CONTENT), anyString(), anyString(), anyString()))
                .thenReturn(Flux.just("Token único"));

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator
                    .processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, times(2))
                    .send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();

            // Verify fallback done payload (assistantMessageId = "")
            ArgumentCaptor<Object> jsonCaptor =
                    ArgumentCaptor.forClass(Object.class);
            verify(objectMapper, times(2))
                    .writeValueAsString(jsonCaptor.capture());

            var payloads = jsonCaptor.getAllValues();

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenPayload =
                    (Map<String, Object>) payloads.get(0);
            assertEquals("Token único", tokenPayload.get("token"));

            @SuppressWarnings("unchecked")
            Map<String, Object> donePayload =
                    (Map<String, Object>) payloads.get(1);
            assertEquals(true, donePayload.get("done"));
            assertEquals("Token único", donePayload.get("content"));
            assertEquals("", donePayload.get("assistantMessageId"));
            assertEquals(0, donePayload.get("tokensUsed"));
        }
    }

    // ---------------------------------------------------------------
    // Test 4 — IOException al enviar token SSE
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaCompletarConError_cuandoSendLanzaIOException() throws Exception {
        stubCommonDependencies();
        when(aiEngineService.generateAiResponseStream(
                anyList(), eq(CONTENT), anyString(), anyString(), anyString()))
                .thenReturn(Flux.just("A"));

        try (var mocked = mockConstruction(SseEmitter.class,
                (mock, context) ->
                        doThrow(new IOException("Broken pipe"))
                                .when(mock)
                                .send(any(SseEmitter.SseEventBuilder.class)))) {

            SseEmitter result = orchestrator
                    .processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter, atLeastOnce())
                    .send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).completeWithError(any(IOException.class));
        }
    }

    // ---------------------------------------------------------------
    // Test 5 — stream vacío (Flux.empty)
    // ---------------------------------------------------------------
    @Test
    void processMessageStream_deberiaEmitirDoneConContenidoVacio_cuandoFluxVacio() throws Exception {
        stubCommonDependencies();
        when(aiEngineService.generateAiResponseStream(
                anyList(), eq(CONTENT), anyString(), anyString(), anyString()))
                .thenReturn(Flux.empty());

        try (var mocked = mockConstruction(SseEmitter.class)) {
            SseEmitter result = orchestrator
                    .processMessageStream(USER_ID, PROJECT_ID, request);

            assertNotNull(result);
            SseEmitter mockEmitter = mocked.constructed().get(0);

            verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();

            // Verify done payload has empty content
            ArgumentCaptor<Object> jsonCaptor =
                    ArgumentCaptor.forClass(Object.class);
            verify(objectMapper).writeValueAsString(jsonCaptor.capture());

            @SuppressWarnings("unchecked")
            Map<String, Object> donePayload =
                    (Map<String, Object>) jsonCaptor.getValue();
            assertEquals(true, donePayload.get("done"));
            assertEquals("", donePayload.get("content"));

            // Verify assistant saved with empty content
            ArgumentCaptor<SaveMessageRequest> saveCaptor =
                    ArgumentCaptor.forClass(SaveMessageRequest.class);
            verify(chatService, times(2))
                    .saveMessage(eq(USER_ID), eq(PROJECT_ID),
                            saveCaptor.capture());

            var saveRequests = saveCaptor.getAllValues();
            assertEquals("", saveRequests.get(1).getContent());
        }
    }
}
