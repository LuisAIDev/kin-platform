package com.kinplatform.chat.dto;

import com.kinplatform.kin.export.intent.ExportAction;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ChatResponse {

    private UUID userMessageId;
    private UUID assistantMessageId;
    private String content;
    private Integer tokensUsed;

    /** Acción aditiva de exportación (opcional, {@code null} por defecto). */
    private ExportAction action;
}
