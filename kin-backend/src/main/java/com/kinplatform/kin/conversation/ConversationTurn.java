package com.kinplatform.kin.conversation;

import com.kinplatform.kin.context.Message;

import java.util.List;
import java.util.UUID;

/**
 * Input tipado de un turno de conversación. Refleja los mismos campos que
 * {@code KinMethodCommand} para que el orquestador pueda delegar la ejecución
 * al pipeline sin acoplarse a HTTP.
 */
public record ConversationTurn(
    UUID projectId,
    UUID userId,
    String userMessage,
    List<Message> history,
    String projectTitle,
    String projectDescription,
    String projectCategory
) {

    public ConversationTurn {
        history = (history != null) ? List.copyOf(history) : List.of();
    }
}
