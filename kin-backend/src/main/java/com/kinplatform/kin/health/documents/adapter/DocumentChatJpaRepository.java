package com.kinplatform.kin.health.documents.adapter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de mensajes de conversación por documento (ADR-041).
 */
public interface DocumentChatJpaRepository extends JpaRepository<DocumentChatEntity, UUID> {

    List<DocumentChatEntity> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);

    long deleteByDocumentId(UUID documentId);
}
