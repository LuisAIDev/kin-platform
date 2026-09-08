package com.kinplatform.kin.health.documents.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mensaje de la conversación de IA asociada a un documento clínico
 * (ADR-041). La conversación vive escopada al documento y al propietario:
 * nunca se comparte entre pacientes.
 */
public record DocumentChatMessage(
        UUID id,
        UUID documentId,
        UUID userId,
        DocumentChatRole role,
        String content,
        OffsetDateTime createdAt) {

    public DocumentChatMessage {
        if (id == null || documentId == null || userId == null || role == null) {
            throw new IllegalArgumentException("id/documentId/userId/role no pueden ser null");
        }
        content = content == null ? "" : content;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static DocumentChatMessage of(
            UUID documentId, UUID userId, DocumentChatRole role, String content) {
        return new DocumentChatMessage(
                UUID.randomUUID(), documentId, userId, role, content, OffsetDateTime.now());
    }
}
