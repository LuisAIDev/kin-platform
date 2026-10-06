package com.kinplatform.platform.export.web;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.platform.export.application.ExportOptions;
import com.kinplatform.platform.export.application.ExportResult;
import com.kinplatform.platform.export.application.ProjectExportService;
import com.kinplatform.platform.export.model.ExportFormat;
import com.kinplatform.platform.export.model.ExportMode;
import com.kinplatform.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints de exportación de proyectos (módulo {@code kin.export}).
 *
 * <p>Solo lectura de datos del proyecto propio: la propiedad se verifica en
 * cada endpoint (404 si el proyecto no existe o es ajeno). El historial del
 * chat no es la fuente del documento.</p>
 *
 * <ul>
 *   <li>{@code GET /projects/{projectId}/export}: opciones disponibles.</li>
 *   <li>{@code GET /projects/{projectId}/export/{format}}: documento exportado.</li>
 * </ul>
 */
@RestController
@RequestMapping("/projects/{projectId}/export")
@RequiredArgsConstructor
public class ProjectExportController {

    private final ProjectExportService exportService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ExportOptions> options(Authentication auth, @PathVariable UUID projectId) {
        var userId = AuthenticatedUsers.require(userRepository, auth).getId();
        return ResponseEntity.ok(exportService.options(userId, projectId));
    }

    @GetMapping("/{format}")
    public ResponseEntity<byte[]> export(
            Authentication auth,
            @PathVariable UUID projectId,
            @PathVariable String format,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "templateDocumentId", required = false) UUID templateDocumentId) {
        var userId = AuthenticatedUsers.require(userRepository, auth).getId();
        ExportFormat exportFormat = ExportFormat.parse(format);
        if (exportFormat == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Formato no soportado. Formatos: docx, pdf, markdown");
        }
        ExportMode exportMode = parseMode(mode);
        ExportResult result = exportService.export(userId, projectId, exportFormat, exportMode, templateDocumentId);

        MediaType mediaType = "markdown".equalsIgnoreCase(format)
                ? new MediaType("text", "markdown", StandardCharsets.UTF_8)
                : MediaType.parseMediaType(exportFormat.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(result.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(result.bytes());
    }

    private ExportMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ExportMode.COMPLETE;
        }
        try {
            return ExportMode.valueOf(mode.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modo no soportado. Modos: complete, summary");
        }
    }
}


