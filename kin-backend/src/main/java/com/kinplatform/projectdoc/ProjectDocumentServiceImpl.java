package com.kinplatform.projectdoc;

import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.projectdoc.dto.DocumentResponse;
import com.kinplatform.projectdoc.extract.DocumentExtractionService;
import com.kinplatform.projectdoc.extract.TextExtractionException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación de {@link ProjectDocumentService}.
 *
 * <p>Valida el archivo (tamaño, extensión, contenido), lo persiste con estado
 * {@code PENDIENTE → PROCESANDO → PROCESADO} (o {@code ERROR}) y extrae el
 * texto acotado mediante {@link DocumentExtractionService}. La propiedad del
 * proyecto se valida con el mismo mecanismo del resto de rutas
 * {@code /projects/{id}}.</p>
 */
@Service
public class ProjectDocumentServiceImpl implements ProjectDocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_EXTRACTED_CHARS = 1_000_000;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "txt", "csv");

    private final ProjectDocumentRepository repository;
    private final ProjectRepository projectRepository;
    private final DocumentExtractionService extractionService;

    public ProjectDocumentServiceImpl(
            ProjectDocumentRepository repository,
            ProjectRepository projectRepository,
            DocumentExtractionService extractionService) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.extractionService = extractionService;
    }

    @Override
    @Transactional
    public DocumentResponse upload(UUID userId, UUID projectId, MultipartFile file) {
        requireOwnedProject(userId, projectId);
        validateFile(file);

        ProjectDocument document = ProjectDocument.builder()
                .projectId(projectId)
                .filename(sanitizeFilename(file.getOriginalFilename()))
                .mimeType(resolveMimeType(file))
                .size(file.getSize())
                .status(ProjectDocumentStatus.PENDIENTE)
                .hash(sha256(file))
                .version(1)
                .build();
        document = repository.save(document);

        document.setStatus(ProjectDocumentStatus.PROCESANDO);
        repository.save(document);

        try {
            String extracted = extractionService.extract(file, document.getMimeType());
            document.setExtractedText(truncate(extracted));
            document.setStatus(ProjectDocumentStatus.PROCESADO);
            document.setErrorMessage(null);
        } catch (TextExtractionException ex) {
            document.setStatus(ProjectDocumentStatus.ERROR);
            document.setErrorMessage("No se pudo procesar el documento. Verifica que el archivo sea válido.");
        }

        return toResponse(repository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> listByProject(UUID userId, UUID projectId) {
        requireOwnedProject(userId, projectId);
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<ProjectDocument> findOwned(UUID userId, UUID projectId, UUID documentId) {
        requireOwnedProject(userId, projectId);
        return repository.findById(documentId).filter(document -> document.getProjectId()
                .equals(projectId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de 10 MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre");
        }
        String extension = extension(filename);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Formato no permitido. Formatos soportados: PDF, DOCX, XLSX, TXT, CSV");
        }
    }

    private String resolveMimeType(MultipartFile file) {
        String extension = extension(file.getOriginalFilename());
        String declared = file.getContentType();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            default -> declared != null ? declared : "application/octet-stream";
        };
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String sanitizeFilename(String filename) {
        String base = filename == null ? "" : filename;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        String name = slash >= 0 ? base.substring(slash + 1) : base;
        name = name.trim();
        if (name.length() > 200) {
            name = name.substring(name.length() - 200);
        }
        return name;
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash del documento", e);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_EXTRACTED_CHARS ? text : text.substring(0, MAX_EXTRACTED_CHARS);
    }

    private Project requireOwnedProject(UUID userId, UUID projectId) {
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (project.getUser() == null || !project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project not found");
        }
        return project;
    }

    private DocumentResponse toResponse(ProjectDocument document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .projectId(document.getProjectId())
                .filename(document.getFilename())
                .mimeType(document.getMimeType())
                .size(document.getSize())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
