package com.kinplatform.chat;

import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatMessageResponse saveMessage(UUID userId, UUID projectId, SaveMessageRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project does not belong to this user");
        }

        var message = ChatMessage.builder()
                .project(project)
                .user(user)
                .role(request.getRole())
                .content(request.getContent().trim())
                .metadata(request.getMetadata())
                .tokensUsed(request.getTokensUsed() != null ? request.getTokensUsed() : 0)
                .build();

        message = chatMessageRepository.save(message);
        return toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversationHistory(UUID userId, UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project does not belong to this user");
        }

        return chatMessageRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void clearConversation(UUID userId, UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project does not belong to this user");
        }

        chatMessageRepository.deleteAll(
                chatMessageRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
        );
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .projectId(message.getProject().getId())
                .userId(message.getUser().getId())
                .role(message.getRole())
                .content(message.getContent())
                .metadata(message.getMetadata())
                .tokensUsed(message.getTokensUsed())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
