package com.kinplatform.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ChatResponse {

    private UUID userMessageId;
    private UUID assistantMessageId;
    private String content;
    private Integer tokensUsed;
}
