package com.kinplatform.projectdoc.dto;

import com.kinplatform.projectdoc.ProjectDocumentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Respuesta de un documento del proyecto. No expone el texto extraído: el
 * documento es información del proyecto y no debe enviarse como mensaje de
 * chat.
 */
@Data
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private UUID id;
    private UUID projectId;
    private String filename;
    private String mimeType;
    private long size;
    private ProjectDocumentStatus status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
