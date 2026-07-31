package com.kinplatform.kin.ai;

import com.kinplatform.kin.context.Message;

import java.util.List;

/**
 * Petición ya resuelta para el proveedor de IA: historial de conversación,
 * mensaje del usuario y el system prompt ya ensamblado por el
 * {@link PromptAssembler}.
 */
public record AIRequest(
    List<Message> history,
    String userMessage,
    String systemPrompt
) {
}
