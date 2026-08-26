package com.kinplatform.kin.health.telemedicine.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mensaje de telemedicina (ADR-032).
 *
 * <p>Entidad de dominio inmutable: mensaje asíncrono entre un paciente y su
 * médico asignado. {@code conversationId} identifica el hilo paciente↔médico
 * (determinista: par de ids ordenado). El contenido sensible se cifra en
 * reposo a nivel de persistencia (ver adaptador JPA).</p>
 */
public record Message(
        UUID id,
        UUID senderId,
        UUID receiverId,
        UUID conversationId,
        String content,
        boolean read,
        OffsetDateTime createdAt) {

    public Message {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("senderId/receiverId no pueden ser null");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("emisor y receptor no pueden ser el mismo");
        }
        content = content == null ? "" : content;
        conversationId = conversationId == null ? conversationIdOf(senderId, receiverId) : conversationId;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
    }

    public static Message of(
            UUID id,
            UUID senderId,
            UUID receiverId,
            UUID conversationId,
            String content,
            boolean read,
            OffsetDateTime createdAt) {
        return new Message(id, senderId, receiverId, conversationId, content, read, createdAt);
    }

    /**
     * Id determinista de conversación: el par (a,b) ordenado (independiente de
     * quién envía). Ambos lados del hilo ven la misma conversación.
     */
    public static UUID conversationIdOf(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public boolean belongsTo(UUID userId) {
        return senderId.equals(userId) || receiverId.equals(userId);
    }
}
