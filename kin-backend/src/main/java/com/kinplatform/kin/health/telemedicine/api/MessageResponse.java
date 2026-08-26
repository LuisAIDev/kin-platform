package com.kinplatform.kin.health.telemedicine.api;

import com.kinplatform.kin.health.telemedicine.domain.Message;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mensaje de telemedicina en el endpoint REST (ADR-032).
 *
 * @param mine {@code true} si el mensaje fue enviado por el usuario autenticado
 *             (el frontend no conoce el userId; el backend lo resuelve del JWT).
 */
public record MessageResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        UUID conversationId,
        String content,
        boolean read,
        boolean mine,
        OffsetDateTime createdAt) {

    public static MessageResponse from(Message message, UUID authenticatedUserId) {
        return new MessageResponse(
                message.id(),
                message.senderId(),
                message.receiverId(),
                message.conversationId(),
                message.content(),
                message.read(),
                authenticatedUserId != null && authenticatedUserId.equals(message.senderId()),
                message.createdAt());
    }
}
