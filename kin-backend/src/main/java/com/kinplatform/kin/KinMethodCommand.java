package com.kinplatform.kin;

import com.kinplatform.kin.context.Message;

import java.util.List;
import java.util.UUID;

public record KinMethodCommand(
    UUID projectId,
    UUID userId,
    String userMessage,
    List<Message> history,
    String projectTitle,
    String projectDescription,
    String projectCategory
) {
}

