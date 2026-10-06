package com.kinplatform.platform.chat.dto;

import com.kinplatform.platform.chat.MessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveMessageRequest {

    @NotNull(message = "Role is required")
    private MessageRole role;

    @NotBlank(message = "Content is required")
    private String content;

    private String metadata;

    private Integer tokensUsed;
}


