package com.kinplatform.chat;

import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatOrchestratorService chatOrchestratorService;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            Authentication auth,
            @PathVariable UUID projectId,
            @Valid @RequestBody ChatRequest request
    ) {
        var userId = getAuthenticatedUserId(auth);
        var response = chatOrchestratorService.processMessage(userId, projectId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> saveMessage(
            Authentication auth,
            @PathVariable UUID projectId,
            @Valid @RequestBody SaveMessageRequest request
    ) {
        var userId = getAuthenticatedUserId(auth);
        var response = chatService.saveMessage(userId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(
            Authentication auth,
            @PathVariable UUID projectId
    ) {
        var userId = getAuthenticatedUserId(auth);
        var messages = chatService.getConversationHistory(userId, projectId);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/messages")
    public ResponseEntity<Void> clearConversation(
            Authentication auth,
            @PathVariable UUID projectId
    ) {
        var userId = getAuthenticatedUserId(auth);
        chatService.clearConversation(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    private UUID getAuthenticatedUserId(Authentication auth) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        return user.getId();
    }
}
