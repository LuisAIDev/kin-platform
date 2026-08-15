package com.kinplatform.projectdoc;

import com.kinplatform.projectdoc.dto.DocumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Servicio de documentos del proyecto (subida, extracción y consulta). */
public interface ProjectDocumentService {

    DocumentResponse upload(UUID userId, UUID projectId, MultipartFile file);

    List<DocumentResponse> listByProject(UUID userId, UUID projectId);
}
