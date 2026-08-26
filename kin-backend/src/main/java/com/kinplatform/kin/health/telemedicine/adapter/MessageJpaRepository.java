package com.kinplatform.kin.health.telemedicine.adapter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de mensajes de telemedicina (ADR-032).
 */
public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {

    List<MessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<MessageEntity> findBySenderIdOrderByCreatedAtAsc(UUID senderId);

    List<MessageEntity> findByReceiverIdOrderByCreatedAtAsc(UUID receiverId);

    long countByReceiverIdAndReadFalse(UUID receiverId);
}
