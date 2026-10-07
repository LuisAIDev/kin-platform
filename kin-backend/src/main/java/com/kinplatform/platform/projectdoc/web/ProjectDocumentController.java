package com.kinplatform.platform.projectdoc.web;

import com.kinplatform.platform.projectdoc.ProjectDocumentService;
import com.kinplatform.platform.projectdoc.dto.DocumentResponse;
import com.kinplatform.common.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de documentos del proyecto: {@code POST/GET
 * /projects/{projectId}/documents}. El ownership se valida en el servicio.
 */
@RestController
@RequestMapping("/projects/{projectId}/documents")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService documentService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            Authentication auth, @PathVariable UUID projectId, @RequestParam("file") MultipartFile file) {
        UUID userId = getAuthenticatedUserId(auth);
        return ResponseEntity.ok(documentService.upload(userId, projectId, file));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(Authentication auth, @PathVariable UUID projectId) {
        UUID userId = getAuthenticatedUserId(auth);
        return ResponseEntity.ok(documentService.listByProject(userId, projectId));
    }

    private UUID getAuthenticatedUserId(Authentication auth) {
        return com.kinplatform.common.security.AuthenticatedUsers.require(userRepository, auth)
                .getId();
    }
}



