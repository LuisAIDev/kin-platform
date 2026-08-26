package com.kinplatform.kin.health.telemedicine.port;

import com.kinplatform.kin.health.telemedicine.domain.Message;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de mensajes de telemedicina (ADR-032).
 *
 * <p>La infraestructura lo implementa con JPA (tabla {@code messages}). El
 * contenido sensible se cifra en reposo a nivel de adaptador.</p>
 */
public interface MessageRepository {

    Message save(Message message);

    Optional<Message> findById(UUID id);

    List<Message> findByConversationId(UUID conversationId);

    List<Message> findBySenderId(UUID userId);

    List<Message> findByReceiverId(UUID userId);

    /** Mensajes no leídos dirigidos al usuario (contador de notificaciones). */
    long countUnreadByReceiver(UUID userId);
}
