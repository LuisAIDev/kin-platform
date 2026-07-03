package com.kinplatform.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.AiEngineService;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.project.Project;
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

@Service
@RequiredArgsConstructor
public class ChatOrchestratorServiceImpl implements ChatOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestratorServiceImpl.class);
    private static final long SSE_TIMEOUT = 180_000L;

    private final ChatService chatService;
    private final AiEngineService aiEngineService;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatResponse processMessage(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId, userMessage);
        var aiResponse = aiEngineService.generateAiResponse(
                history, request.getContent(),
                project.getTitle(), project.getDescription(), project.getCategory().name()
        );
        var assistantMessage = saveAssistantMessage(userId, projectId, aiResponse);

        return ChatResponse.builder()
                .userMessageId(userMessage.getId())
                .assistantMessageId(assistantMessage.getId())
                .content(aiResponse)
                .tokensUsed(assistantMessage.getTokensUsed())
                .build();
    }

    @Override
    public SseEmitter processMessageStream(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId, userMessage);

        var emitter = new SseEmitter(SSE_TIMEOUT);
        var fullContent = new StringBuilder();

        var flux = aiEngineService.generateAiResponseStream(
                history, request.getContent(),
                project.getTitle(), project.getDescription(), project.getCategory().name()
        );

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

    private Project findProject(UUID userId, UUID projectId) {
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

    private List<com.kinplatform.chat.ChatMessage> loadHistoryForContext(
            UUID userId, UUID projectId, ChatMessageResponse lastMessage
    ) {
        var history = chatService.getConversationHistory(userId, projectId);
        var entities = new ArrayList<com.kinplatform.chat.ChatMessage>();

        for (var msg : history) {
            var role = switch (msg.getRole()) {
                case USER -> "USER";
                case ASSISTANT -> "ASSISTANT";
                case SYSTEM -> "SYSTEM";
            };

            var entity = com.kinplatform.chat.ChatMessage.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build();

            entities.add(entity);
        }

        return entities;
    }
}