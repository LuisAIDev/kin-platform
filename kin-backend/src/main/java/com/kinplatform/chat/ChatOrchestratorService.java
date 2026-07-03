package com.kinplatform.chat;

import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface ChatOrchestratorService {

    ChatResponse processMessage(UUID userId, UUID projectId, ChatRequest request);

    SseEmitter processMessageStream(UUID userId, UUID projectId, ChatRequest request);
}
