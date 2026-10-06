package com.kinplatform.platform.chat;

import com.kinplatform.platform.chat.dto.ChatMessageResponse;
import com.kinplatform.platform.chat.dto.SaveMessageRequest;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatMessageResponse saveMessage(UUID userId, UUID projectId, SaveMessageRequest request);

    List<ChatMessageResponse> getConversationHistory(UUID userId, UUID projectId);

    void clearConversation(UUID userId, UUID projectId);
}


