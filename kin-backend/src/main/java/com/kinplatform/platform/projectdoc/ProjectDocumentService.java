package com.kinplatform.platform.projectdoc;

import com.kinplatform.platform.projectdoc.dto.DocumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Servicio de documentos del proyecto (subida, extracción y consulta). */
public interface ProjectDocumentService {

    DocumentResponse upload(UUID userId, UUID projectId, MultipartFile file);

    List<DocumentResponse> listByProject(UUID userId, UUID projectId);

    /**
     * Devuelve el documento del proyecto verificando ownership (proyecto y
     * documento pertenecientes al usuario). Expone el {@code extractedText}
     * sin revelar documentos de otros proyectos.
     */
    java.util.Optional<ProjectDocument> findOwned(UUID userId, UUID projectId, UUID documentId);
}


