package com.kinplatform.common.method;

import com.kinplatform.common.context.Message;
import com.kinplatform.common.conversation.TurnDirective;

import java.util.List;
import java.util.UUID;

public record KinMethodCommand(
    UUID projectId,
    UUID userId,
    String userMessage,
    List<Message> history,
    String projectTitle,
    String projectDescription,
    String projectCategory,
    TurnDirective directive
) {

    public KinMethodCommand(UUID projectId, UUID userId, String userMessage, List<Message> history,
                            String projectTitle, String projectDescription, String projectCategory) {
        this(projectId, userId, userMessage, history,
            projectTitle, projectDescription, projectCategory, null);
    }
}





