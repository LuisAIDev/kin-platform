package com.kinplatform.platform.chat.dto;

import com.kinplatform.platform.chat.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private UUID id;
    private UUID projectId;
    private UUID userId;
    private MessageRole role;
    private String content;
    private String metadata;
    private Integer tokensUsed;
    private OffsetDateTime createdAt;
}


