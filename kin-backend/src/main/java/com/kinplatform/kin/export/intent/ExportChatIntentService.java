package com.kinplatform.kin.export.intent;

import com.kinplatform.projectdoc.ProjectDocumentService;
import com.kinplatform.projectdoc.ProjectDocumentStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Servicio de intención de exportación desde el chat (frontera Spring).
 *
 * <p>Resuelve el documento de referencia más reciente (solo {@code PROCESADO})
 * del proyecto y delega la detección en el {@link ExportIntentDetector} puro.
 * El documento siempre pertenece al proyecto autenticado; la exportación
 * posterior vuelve a verificar ownership.</p>
 */
@Component
public class ExportChatIntentService {

    private final ProjectDocumentService documentService;
    private final ExportIntentDetector detector;

    public ExportChatIntentService(ProjectDocumentService documentService) {
        this.documentService = documentService;
        this.detector = new ExportIntentDetector();
    }

    /**
     * Detecta la intención de exportación del mensaje.
     *
     * @param userId    usuario autenticado
     * @param projectId proyecto del turno
     * @param message   contenido del mensaje del usuario
     * @return acción de exportación o {@code null} si no hay intención
     */
    public ExportAction detect(UUID userId, UUID projectId, String message) {
        if (userId == null || projectId == null) {
            return null;
        }
        DocumentReference latest = latestProcessedDocument(userId, projectId);
        ExportAction action = detector.detect(message, latest == null ? null : latest.id());
        if (action == null || action.templateDocumentId() == null) {
            return action;
        }
        return new ExportAction(
                action.type(), action.format(), action.templateDocumentId(), latest == null ? "" : latest.filename());
    }

    private DocumentReference latestProcessedDocument(UUID userId, UUID projectId) {
        try {
            return documentService.listByProject(userId, projectId).stream()
                    .filter(doc -> doc.getStatus() == ProjectDocumentStatus.PROCESADO)
                    .map(doc -> new DocumentReference(doc.getId(), doc.getFilename()))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private record DocumentReference(UUID id, String filename) {}
}
