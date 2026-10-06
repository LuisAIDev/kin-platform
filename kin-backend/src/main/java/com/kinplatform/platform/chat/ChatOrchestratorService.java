package com.kinplatform.platform.chat;

import com.kinplatform.platform.chat.dto.ChatRequest;
import com.kinplatform.platform.chat.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface ChatOrchestratorService {

    ChatResponse processMessage(UUID userId, UUID projectId, ChatRequest request);

    SseEmitter processMessageStream(UUID userId, UUID projectId, ChatRequest request);
}


