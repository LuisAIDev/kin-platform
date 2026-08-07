package com.kinplatform.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.guardrails.PromptGuardrail;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.kin.context.Message;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.project.ProjectRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Orquestador de chat. Tras la consolidación del runtime (Fase 5.2.1) y del
 * Conversation Orchestrator (Fase 5.6, ADR-013) NO contiene lógica de negocio
 * ni flujos ad-hoc: ambos endpoints ({@code /chat} y {@code /chat/stream})
 * delegan en {@link ConversationOrchestrator}, que resuelve la directiva en
 * Java y delega la ejecución en el pipeline. Este servicio solo se ocupa de la
 * I/O HTTP: persistir los mensajes y emitir el SSE.
 */
@Service
public class ChatOrchestratorServiceImpl implements ChatOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestratorServiceImpl.class);
    private static final long SSE_TIMEOUT = 180_000L;
    private static final String BLOCKED_MESSAGE =
            "No puedo procesar esa solicitud: parece contener instrucciones que intentan "
                    + "manipular el comportamiento del asistente. Por favor, reformúlala.";

    private final ChatService chatService;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;
    private final ConversationOrchestrator conversationOrchestrator;
    private final PromptGuardrail promptGuardrail;

    @Autowired
    public ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator,
            PromptGuardrail promptGuardrail) {
        this.chatService = chatService;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
        this.conversationOrchestrator = conversationOrchestrator;
        this.promptGuardrail = promptGuardrail;
    }

    /**
     * Constructor de compatibilidad (4 args): usaba la firma previa antes de la
     * capa de guardrails (Fase 15). Activa los guardrails por defecto.
     */
    public ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator) {
        this(chatService, projectRepository, objectMapper, conversationOrchestrator, new PromptGuardrail());
    }

    /**
     * Sin {@code @Transactional}: la llamada a la IA (orchestrate) es I/O externa
     * lenta y no debe retener una conexi�n JDBC durante segundos. La persistencia
     * de mensajes se realiza a trav�s de {@link ChatService}, que s� aplica sus
     * propias transacciones de corta vida por operaci�n (saveMessage,
     * getConversationHistory).
     */
    @Override
    public ChatResponse processMessage(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        if (isBlocked(request.getContent())) {
            var userMessage = saveUserMessage(userId, projectId, request.getContent());
            var assistantMessage = saveAssistantMessage(userId, projectId, BLOCKED_MESSAGE);
            return ChatResponse.builder()
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .content(BLOCKED_MESSAGE)
                    .tokensUsed(0)
                    .build();
        }
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId);
        var turn = new ConversationTurn(
                projectId,
                userId,
                request.getContent(),
                history,
                project.getTitle(),
                project.getDescription(),
                project.getCategory() != null ? project.getCategory().getName() : null);
        var result = conversationOrchestrator.orchestrate(turn);
        log.info(
                "=== KIN METHOD RESULT === action={}, phase={}, chars={}, events={}, validationAccepted={}",
                result.decision() != null ? result.decision().action() : null,
                result.directive() != null ? result.directive().phase() : null,
                result.aiResponse() != null ? result.aiResponse().length() : 0,
                result.events().size(),
                result.validation() != null ? result.validation().accepted() : null);
        var assistantMessage = saveAssistantMessage(userId, projectId, result.aiResponse());
        return ChatResponse.builder()
                .userMessageId(userMessage.getId())
                .assistantMessageId(assistantMessage.getId())
                .content(result.aiResponse())
                .tokensUsed(assistantMessage.getTokensUsed())
                .build();
    }

    @Override
    public SseEmitter processMessageStream(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        if (isBlocked(request.getContent())) {
            saveUserMessage(userId, projectId, request.getContent());
            return blockedEmitter();
        }
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId);
        var turn = new ConversationTurn(
                projectId,
                userId,
                request.getContent(),
                history,
                project.getTitle(),
                project.getDescription(),
                project.getCategory() != null ? project.getCategory().getName() : null);

        log.info(
                "=== STREAMING AI RESPONSE === project={}, userId={}, historySize={}",
                projectId,
                userId,
                history.size());

        var flux = conversationOrchestrator.orchestrateStream(turn);
        var emitter = new SseEmitter(SSE_TIMEOUT);
        var fullContent = new StringBuilder();

        flux.subscribe(
                token -> {
                    fullContent.append(token);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(objectMapper.writeValueAsString(Map.of("token", token))));
                    } catch (IOException e) {
                        log.error("Failed to send SSE event", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("Stream error, sending failure notification", error);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(objectMapper.writeValueAsString(Map.of("error", error.getMessage()))));
                    } catch (IOException e) {
                        // ignore
                    }
                    emitter.complete();
                },
                () -> {
                    var finalContent = fullContent.toString();
                    log.info("=== AI RESPONSE RECEIVED === chars={}", finalContent.length());
                    try {
                        var assistantMessage = saveAssistantMessage(userId, projectId, finalContent);
                        var donePayload = Map.of(
                                "done", true,
                                "userMessageId", userMessage.getId().toString(),
                                "assistantMessageId", assistantMessage.getId().toString(),
                                "content", finalContent,
                                "tokensUsed", assistantMessage.getTokensUsed());
                        emitter.send(
                                SseEmitter.event().name("done").data(objectMapper.writeValueAsString(donePayload)));
                    } catch (Exception e) {
                        log.error("Failed to save assistant message or send done event", e);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(objectMapper.writeValueAsString(Map.of(
                                            "done",
                                            true,
                                            "content",
                                            finalContent,
                                            "userMessageId",
                                            userMessage.getId().toString(),
                                            "assistantMessageId",
                                            "",
                                            "tokensUsed",
                                            0))));
                        } catch (IOException ex) {
                            // ignore
                        }
                    } finally {
                        emitter.complete();
                    }
                });

        return emitter;
    }

    private boolean isBlocked(String content) {
        if (promptGuardrail == null || content == null || content.isBlank()) {
            return false;
        }
        boolean blocked = promptGuardrail.analyze(content).blocked();
        if (blocked) {
            log.warn("=== GUARDRAIL BLOCKED === input contained prompt injection signals");
        }
        return blocked;
    }

    private SseEmitter blockedEmitter() {
        var emitter = new SseEmitter(SSE_TIMEOUT);
        try {
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data(objectMapper.writeValueAsString(Map.of("token", BLOCKED_MESSAGE))));
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "done",
                            true,
                            "userMessageId",
                            "",
                            "assistantMessageId",
                            "",
                            "content",
                            BLOCKED_MESSAGE,
                            "tokensUsed",
                            0))));
        } catch (IOException e) {
            log.error("Failed to emit guardrail blocked event", e);
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    private com.kinplatform.project.Project findProject(UUID userId, UUID projectId) {
        var project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project does not belong to this user");
        }
        return project;
    }

    private ChatMessageResponse saveUserMessage(UUID userId, UUID projectId, String content) {
        var request = new SaveMessageRequest();
        request.setRole(MessageRole.USER);
        request.setContent(content);
        return chatService.saveMessage(userId, projectId, request);
    }

    private ChatMessageResponse saveAssistantMessage(UUID userId, UUID projectId, String content) {
        var request = new SaveMessageRequest();
        request.setRole(MessageRole.ASSISTANT);
        request.setContent(content);
        return chatService.saveMessage(userId, projectId, request);
    }

    private List<Message> loadHistoryForContext(UUID userId, UUID projectId) {
        var history = chatService.getConversationHistory(userId, projectId);
        var messages = new ArrayList<Message>(history.size());
        for (var msg : history) {
            messages.add(new Message(msg.getRole().name(), msg.getContent()));
        }
        return messages;
    }
}
