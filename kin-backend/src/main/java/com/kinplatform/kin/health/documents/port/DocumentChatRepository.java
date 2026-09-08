package com.kinplatform.kin.health.documents.port;

import com.kinplatform.kin.health.documents.domain.DocumentChatMessage;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de persistencia de la conversación de análisis por documento (ADR-041).
 */
public interface DocumentChatRepository {

    DocumentChatMessage save(DocumentChatMessage message);

    /** Historial cronológico (ascendente) de la conversación de un documento. */
    List<DocumentChatMessage> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
