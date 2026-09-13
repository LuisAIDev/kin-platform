package com.kinplatform.kin.health.documents.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.documents.domain.DocumentChatMessage;
import com.kinplatform.kin.health.documents.domain.DocumentChatRole;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conversación de IA sobre un documento clínico (Centro de Documentos Clínicos,
 * ADR-041). Todos los endpoints validan el ownership del documento en el
 * servicio: un paciente solo puede conversar sobre SUS documentos.
 */
@RestController
@RequestMapping({"/health/documents/{documentId}/chat", "/medical/documents/{documentId}/chat"})
public class DocumentChatController {

    private static final Logger log = LoggerFactory.getLogger(DocumentChatController.class);

    private final DocumentChatService chatService;
    private final UserRepository userRepository;

    public DocumentChatController(DocumentChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatTurnResponse> send(
            Authentication authentication,
            @PathVariable UUID documentId,
            @RequestBody SendMessageRequest request) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        DocumentChatService.ChatTurn turn = chatService.sendMessage(
                user.getId(), documentId, user.getRole() == UserRole.ADMIN, request.content());
        log.info("=== DOCUMENT CHAT === document={}, user={}", documentId, user.getId());
        return ResponseEntity.ok(ChatTurnResponse.from(turn));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<DocumentChatMessageResponse>> history(
            Authentication authentication, @PathVariable UUID documentId) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        List<DocumentChatMessageResponse> messages = chatService.history(
                        user.getId(), documentId, user.getRole() == UserRole.ADMIN)
                .stream()
                .map(DocumentChatMessageResponse::from)
                .toList();
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/messages")
    public ResponseEntity<Void> clear(Authentication authentication, @PathVariable UUID documentId) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        chatService.clearConversation(user.getId(), documentId, user.getRole() == UserRole.ADMIN);
        return ResponseEntity.noContent().build();
    }

    public record SendMessageRequest(String content) {}

    public record DocumentChatMessageResponse(
            UUID id,
            UUID documentId,
            UUID userId,
            DocumentChatRole role,
            String content,
            OffsetDateTime createdAt) {
        static DocumentChatMessageResponse from(DocumentChatMessage message) {
            return new DocumentChatMessageResponse(
                    message.id(), message.documentId(), message.userId(),
                    message.role(), message.content(), message.createdAt());
        }
    }

    public record ChatTurnResponse(
            DocumentChatMessageResponse userMessage,
            DocumentChatMessageResponse assistantMessage,
            String verificationStatus) {
        static ChatTurnResponse from(DocumentChatService.ChatTurn turn) {
            return new ChatTurnResponse(
                    DocumentChatMessageResponse.from(turn.userMessage()),
                    DocumentChatMessageResponse.from(turn.assistantMessage()),
                    turn.verificationStatus() == null ? "" : turn.verificationStatus());
        }
    }
}
