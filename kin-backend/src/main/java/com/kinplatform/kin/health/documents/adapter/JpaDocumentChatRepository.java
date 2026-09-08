package com.kinplatform.kin.health.documents.adapter;

import com.kinplatform.kin.health.documents.domain.DocumentChatMessage;
import com.kinplatform.kin.health.documents.domain.DocumentChatRole;
import com.kinplatform.kin.health.documents.port.DocumentChatRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link DocumentChatRepository} (ADR-041).
 */
@Component
public class JpaDocumentChatRepository implements DocumentChatRepository {

    private final DocumentChatJpaRepository repository;

    public JpaDocumentChatRepository(DocumentChatJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DocumentChatMessage save(DocumentChatMessage message) {
        DocumentChatEntity entity = repository.findById(message.id()).orElseGet(DocumentChatEntity::new);
        entity.setId(message.id());
        entity.setDocumentId(message.documentId());
        entity.setUserId(message.userId());
        entity.setRole(message.role());
        entity.setContent(message.content());
        entity.setCreatedAt(message.createdAt());
        repository.save(entity);
        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChatMessage> findByDocumentIdOrderByCreatedAtAsc(UUID documentId) {
        if (documentId == null) {
            return List.of();
        }
        return repository.findByDocumentIdOrderByCreatedAtAsc(documentId).stream()
                .map(JpaDocumentChatRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByDocumentId(UUID documentId) {
        if (documentId != null) {
            repository.deleteByDocumentId(documentId);
        }
    }

    static DocumentChatMessage toDomain(DocumentChatEntity e) {
        return new DocumentChatMessage(
                e.getId(), e.getDocumentId(), e.getUserId(), e.getRole(), e.getContent(), e.getCreatedAt());
    }
}
