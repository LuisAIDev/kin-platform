package com.kinplatform.kin.health.aiassist.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AIAssistRequest(
        UUID id,
        AIAssistType type,
        String inputData,
        String response,
        OffsetDateTime timestamp,
        UUID userId,
        UUID patientId,
        String context) {

    public AIAssistRequest {
        if (id == null || type == null || userId == null || patientId == null) {
            throw new IllegalArgumentException("id/type/userId/patientId no pueden ser null");
        }
        inputData = inputData == null ? "" : inputData;
        response = response == null ? "" : response;
        timestamp = timestamp == null ? OffsetDateTime.now() : timestamp;
        context = context == null ? "" : context;
    }

    public static AIAssistRequest of(AIAssistType type, String inputData, String response,
                                     UUID userId, UUID patientId, String context) {
        return new AIAssistRequest(
                UUID.randomUUID(), type, inputData, response, OffsetDateTime.now(),
                userId, patientId, context);
    }
}
