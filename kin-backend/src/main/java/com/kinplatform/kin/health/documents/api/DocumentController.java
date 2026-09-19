package com.kinplatform.kin.health.documents.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.security.PatientAccess;
import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.validation.constraints.NotNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints REST de documentos clínicos compartidos (ADR-036).
 *
 * <p>Médico: sube y lista documentos de un paciente activo, los elimina y los
 * descarga. Paciente: lista y descarga sus propios documentos. Toda operación
 * sobre un paciente exige relación {@code ACTIVE} (Área 5) y queda auditada.</p>
 */
@RestController
@RequestMapping({
    "/health/documents",
    "/medical/documents"
})
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            Authentication authentication,
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam("patientId") UUID patientId,
            @RequestParam(required = false) String description) {
        User user = requirePhysician(authentication);
        ClinicalDocument document;
        try {
            document = documentService.uploadDocument(
                    user.getId(), patientId, file.getOriginalFilename(), file.getContentType(),
                    file.getBytes(), description);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo subir el documento: " + e.getMessage(), e);
        }
        log.info("=== DOCUMENT UPLOAD === physician={}, patient={}", user.getId(), patientId);
        return ResponseEntity.status(201).body(DocumentResponse.from(document));
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<DocumentResponse>> documentsForPatient(
            Authentication authentication, @PathVariable UUID patientId) {
        User user = requirePhysician(authentication);
        List<DocumentResponse> list = documentService.listDocumentsForPatient(user.getId(), patientId).stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/my")
    public ResponseEntity<List<DocumentResponse>> myDocuments(Authentication authentication) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        List<DocumentResponse> list = documentService.listMyDocuments(patientId).stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Subida de un documento clínico por el propio paciente (Centro de
     * Documentos Clínicos). El documento queda asociado al paciente autenticado:
     * nunca se acepta un {@code patientId} distinto en esta ruta, por lo que un
     * paciente no puede subir (ni luego acceder) a documentos de otro.
     */
    @PostMapping(value = "/my/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadOwn(
            Authentication authentication,
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam(required = false) String description) {
        User user = requirePatient(authentication);
        ClinicalDocument document;
        try {
            document = documentService.uploadOwnDocument(
                    user.getId(), file.getOriginalFilename(), file.getContentType(),
                    file.getBytes(), description);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo subir el documento: " + e.getMessage(), e);
        }
        log.info("=== DOCUMENT SELF-UPLOAD === patient={}", user.getId());
        return ResponseEntity.status(201).body(DocumentResponse.from(document));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(Authentication authentication, @PathVariable UUID documentId) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        DocumentService.DownloadedDocument downloaded = documentService.downloadDocument(documentId, userId);
        return ResponseEntity.ok()
                .contentType(parseMediaType(downloaded.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encode(downloaded.fileName()))
                .body(downloaded.content());
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID documentId) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        UUID userId = user.getId();

        // Paciente: soft delete (oculta de su vista)
        if (user.getRole() != UserRole.PHYSICIAN && user.getRole() != UserRole.ADMIN) {
            documentService.hideFromPatient(documentId, userId);
        } else {
            // Médico/ADMIN: hard delete (solo para documentos que subió)
            documentService.deleteDocument(documentId, userId);
        }
        return ResponseEntity.noContent().build();
    }

    private User requirePhysician(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (user.getRole() != UserRole.PHYSICIAN && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Se requiere rol de médico para esta operación");
        }
        return user;
    }

    private User requirePatient(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (user.getRole() != UserRole.ADMIN && !PatientAccess.isPatient(user)) {
            throw new AccessDeniedException("Se requiere rol de paciente para esta operación");
        }
        return user;
    }

    private static MediaType parseMediaType(String mime) {
        if (mime == null || mime.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mime);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record DocumentResponse(
            UUID id,
            String fileName,
            long fileSize,
            String mimeType,
            UUID patientId,
            UUID physicianId,
            String description,
            DocumentStatus status,
            OffsetDateTime uploadedAt,
            boolean analyzable) {
        static DocumentResponse from(ClinicalDocument d) {
            return new DocumentResponse(
                    d.id(), d.fileName(), d.fileSize(), d.mimeType(), d.patientId(), d.physicianId(),
                    d.description(), d.status(), d.uploadedAt(), d.hasExtractedText());
        }
    }
}
