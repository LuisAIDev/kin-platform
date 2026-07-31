package com.kinplatform.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.KinMethodCommand;
import com.kinplatform.kin.context.Message;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestador de chat. Tras la consolidación del runtime (Fase 5.2.1) NO
 * contiene lógica de negocio ni flujos ad-hoc: ambos endpoints ({@code /chat}
 * y {@code /chat/stream}) delegan en {@link KinMethod}, el único punto de
 * entrada del pipeline. Este servicio solo se ocupa de la I/O HTTP: persistir
 * los mensajes y emitir el SSE.
 */
@Service
@RequiredArgsConstructor
public class ChatOrchestratorServiceImpl implements ChatOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestratorServiceImpl.class);
    private static final long SSE_TIMEOUT = 180_000L;

    private final ChatService chatService;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;
    private final KinMethod kinMethod;

    @Override
    @Transactional
    public ChatResponse processMessage(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId);
        var command = new KinMethodCommand(
            projectId, userId, request.getContent(), history,
            project.getTitle(), project.getDescription(), project.getCategory() != null ? project.getCategory().name() : null
        );
        var result = kinMethod.execute(command);
        log.info("=== KIN METHOD RESULT === action={}, chars={}, events={}",
                result.decision() != null ? result.decision().action() : null,
                result.aiResponse() != null ? result.aiResponse().length() : 0,
                result.events().size());
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
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId);
        var command = new KinMethodCommand(
            projectId, userId, request.getContent(), history,
            project.getTitle(), project.getDescription(), project.getCategory() != null ? project.getCategory().name() : null
        );

        log.info("=== STREAMING AI RESPONSE === project={}, userId={}, historySize={}",
                projectId, userId, history.size());

        var flux = kinMethod.executeStream(command);
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
                                "tokensUsed", assistantMessage.getTokensUsed()
                        );
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(objectMapper.writeValueAsString(donePayload)));
                    } catch (Exception e) {
                        log.error("Failed to save assistant message or send done event", e);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(objectMapper.writeValueAsString(Map.of(
                                            "done", true,
                                            "content", finalContent,
                                            "userMessageId", userMessage.getId().toString(),
                                            "assistantMessageId", "",
                                            "tokensUsed", 0
                                    ))));
                        } catch (IOException ex) {
                            // ignore
                        }
                    } finally {
                        emitter.complete();
                    }
                }
        );

        return emitter;
    }

    private com.kinplatform.project.Project findProject(UUID userId, UUID projectId) {
        var project = projectRepository.findById(projectId)
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
