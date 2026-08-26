package com.kinplatform.kin.health.telemedicine.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA de mensaje de telemedicina (tabla {@code messages}, ADR-032).
 * El contenido se almacena cifrado en reposo ({@link ContentCipher}).
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (read == null) {
            read = false;
        }
    }
}
