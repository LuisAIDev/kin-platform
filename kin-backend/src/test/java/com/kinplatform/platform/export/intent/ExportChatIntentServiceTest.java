package com.kinplatform.platform.export.intent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kinplatform.platform.projectdoc.ProjectDocumentService;
import com.kinplatform.platform.projectdoc.ProjectDocumentStatus;
import com.kinplatform.platform.projectdoc.dto.DocumentResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * El servicio resuelve el documento de referencia (solo PROCESADO) del
 * proyecto del usuario y delega la detección en el detector puro.
 */
class ExportChatIntentServiceTest {

    private final ProjectDocumentService documentService = mock(ProjectDocumentService.class);
    private final ExportChatIntentService service = new ExportChatIntentService(documentService);

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void adjuntaElDocumentoProcesadoMasReciente() {
        UUID docId = UUID.randomUUID();
        when(documentService.listByProject(userId, projectId))
                .thenReturn(List.of(
                        DocumentResponse.builder()
                                .id(docId)
                                .projectId(projectId)
                                .filename("Plantilla.docx")
                                .mimeType("application/vnd...")
                                .size(10)
                                .status(ProjectDocumentStatus.PROCESADO)
                                .build(),
                        DocumentResponse.builder()
                                .id(UUID.randomUUID())
                                .projectId(projectId)
                                .filename("roto.pdf")
                                .mimeType("application/pdf")
                                .size(20)
                                .status(ProjectDocumentStatus.ERROR)
                                .build()));

        ExportAction action =
                service.detect(userId, projectId, "Descárgame este proyecto usando el documento que subí");

        assertNotNull(action);
        assertEquals(docId, action.templateDocumentId());
    }

    @Test
    void sinDocumentosNoAdjuntaPlantilla() {
        when(documentService.listByProject(userId, projectId)).thenReturn(List.of());

        ExportAction action = service.detect(userId, projectId, "Descárgame el proyecto en PDF");

        assertNotNull(action);
        assertNull(action.templateDocumentId());
    }

    @Test
    void sinIntencionDevuelveNull() {
        when(documentService.listByProject(userId, projectId)).thenReturn(List.of());

        assertNull(service.detect(userId, projectId, "Hola KIN"));
    }
}


