package com.kinplatform.chat;

import com.kinplatform.ai.AiEngineService;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatOrchestratorServiceImpl implements ChatOrchestratorService {

    private final ChatService chatService;
    private final AiEngineService aiEngineService;
    private final ProjectRepository projectRepository;

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