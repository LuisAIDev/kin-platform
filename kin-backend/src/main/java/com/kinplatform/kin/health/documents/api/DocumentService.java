package com.kinplatform.kin.health.documents.api;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.documents.config.DocumentProperties;
import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.kin.health.documents.event.DocumentDeletedEvent;
import com.kinplatform.kin.health.documents.event.DocumentUploadedEvent;
import com.kinplatform.kin.health.documents.exception.QuotaExceededException;
import com.kinplatform.kin.health.documents.infrastructure.ClinicalDocumentTextExtractor;
import com.kinplatform.kin.health.documents.infrastructure.DocumentStorage;
import com.kinplatform.kin.health.documents.port.ClinicalDocumentRepository;
import com.kinplatform.kin.health.documents.port.DocumentStorageQuotaPort;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación de documentos clínicos compartidos (ADR-036).
 *
 * <p>Subir, listar, descargar y eliminar documentos de un paciente. Toda
 * operación exige relación {@code ACTIVE} (Área 5) y queda auditada (Área 12).
 * Los archivos se guardan en almacenamiento local y los registros en BD.</p>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final ClinicalDocumentRepository documentRepository;
    private final DocumentStorage storage;
    private final RelationshipAccessValidator accessValidator;
    private final AuditService auditService;
    private final DocumentProperties properties;
    private final DomainEventBus eventBus;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ClinicalDocumentTextExtractor textExtractor;
    private final DocumentStorageQuotaPort documentStorageQuotaPort;

    public DocumentService(
            ClinicalDocumentRepository documentRepository,
            DocumentStorage storage,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            DocumentProperties properties,
            DocumentStorageQuotaPort documentStorageQuotaPort) {
        this(documentRepository, storage, accessValidator, auditService, properties, null, null, null, documentStorageQuotaPort);
    }

    /** Compatibilidad de tests: sin extractor de texto. */
    public DocumentService(
            ClinicalDocumentRepository documentRepository,
            DocumentStorage storage,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            DocumentProperties properties,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher,
            DocumentStorageQuotaPort documentStorageQuotaPort) {
        this(documentRepository, storage, accessValidator, auditService, properties, eventBus,
                outboxEventPublisher, null, documentStorageQuotaPort);
    }

    @Autowired
    public DocumentService(
            ClinicalDocumentRepository documentRepository,
            DocumentStorage storage,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            DocumentProperties properties,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher,
            ClinicalDocumentTextExtractor textExtractor,
            DocumentStorageQuotaPort documentStorageQuotaPort) {
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.accessValidator = accessValidator;
        this.auditService = auditService;
        this.properties = properties;
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
        this.textExtractor = textExtractor;
        this.documentStorageQuotaPort = documentStorageQuotaPort;
    }

    @Transactional
    public ClinicalDocument uploadDocument(
            UUID physicianId,
            UUID patientId,
            String fileName,
            String mimeType,
            byte[] content,
            String description) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);
        ClinicalDocument document = storeAndPersist(
                physicianId, patientId, physicianId, fileName, mimeType, content, description);
        auditService.logAccess(physicianId, AuditAction.UPLOAD_DOCUMENT, AuditResourceType.DOCUMENTO, document.id(),
                patientId, Map.of("fileName", document.fileName()));
        log.info("DocumentService: documento {} subido por médico {} para paciente {}",
                document.id(), physicianId, patientId);
        return document;
    }

    /**
     * Subida de un documento clínico por el propio paciente (Centro de
     * Documentos Clínicos). El paciente SIEMPRE queda como dueño del documento
     * ({@code patientId = userId}) y sin médico asociado: solo él podrá
     * consultarlo y analizarlo. No exige relación (el usuario actúa sobre su
     * propia información).
     */
    @Transactional
    public ClinicalDocument uploadOwnDocument(
            UUID patientId,
            String fileName,
            String mimeType,
            byte[] content,
            String description) {
        requireEnabled();
        // Validar cuota de almacenamiento antes de subir
        if (!documentStorageQuotaPort.canUpload(patientId, content.length)) {
            throw new QuotaExceededException("Has superado el límite de almacenamiento de tu plan.");
        }
        ClinicalDocument document = storeAndPersist(
                patientId, patientId, null, fileName, mimeType, content, description);
        auditService.logAccess(patientId, AuditAction.UPLOAD_DOCUMENT, AuditResourceType.DOCUMENTO, document.id(),
                patientId, Map.of("fileName", document.fileName()));
        log.info("DocumentService: documento propio {} subido por paciente {}", document.id(), patientId);
        return document;
    }

    private ClinicalDocument storeAndPersist(
            UUID uploadedBy,
            UUID patientId,
            UUID physicianId,
            String fileName,
            String mimeType,
            byte[] content,
            String description) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (content.length > properties.getMaxFileSize()) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido");
        }

        String safeName = sanitizeFileName(fileName);
        String storageKey = UUID.randomUUID() + "_" + safeName;
        storage.store(storageKey, content);

        String extractedText = "";
        if (textExtractor != null) {
            extractedText = textExtractor.extract(content, safeName, mimeType);
        }
        ClinicalDocument document = ClinicalDocument.of(
                UUID.randomUUID(),
                safeName,
                content.length,
                mimeType,
                storageKey,
                uploadedBy,
                patientId,
                physicianId,
                description,
                DocumentStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                extractedText);
        ClinicalDocument saved = documentRepository.save(document);
        publish(new DocumentUploadedEvent(saved.id(), patientId, physicianId, safeName));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClinicalDocument> listDocumentsForPatient(UUID physicianId, UUID patientId) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);
        return documentRepository.findActiveByPatientIdAndPhysicianId(patientId, physicianId);
    }

    @Transactional(readOnly = true)
    public List<ClinicalDocument> listMyDocuments(UUID patientId) {
        requireEnabled();
        if (patientId == null) {
            return List.of();
        }
        return documentRepository.findActiveByPatientId(patientId);
    }

    /** Resultado de una descarga (bytes + metadatos). */
    public record DownloadedDocument(byte[] content, String fileName, String mimeType) {}

    @Transactional
    public DownloadedDocument downloadDocument(UUID documentId, UUID userId) {
        requireEnabled();
        ClinicalDocument document = requireActiveDocument(documentId);
        requireAccess(document, userId, "Descarga denegada: no tiene acceso a este documento");
        byte[] content;
        try {
            content = storage.load(document.storageKey());
        } catch (RuntimeException e) {
            throw new DocumentNotFoundException(documentId);
        }
        auditService.logAccess(userId, AuditAction.DOWNLOAD_DOCUMENT, AuditResourceType.DOCUMENTO, documentId,
                document.patientId(), Map.of("fileName", document.fileName()));
        log.info("DocumentService: documento {} descargado por {}", documentId, userId);
        return new DownloadedDocument(content, document.fileName(), document.mimeType());
    }

    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        requireEnabled();
        ClinicalDocument document = requireActiveDocument(documentId);
        if (!document.uploadedBy().equals(userId) && !document.physicianId().equals(userId)) {
            throw new DocumentAccessDeniedException("Solo el médico que subió el documento puede eliminarlo");
        }
        documentRepository.save(document.deleted());
        storage.delete(document.storageKey());
        publish(new DocumentDeletedEvent(documentId, document.patientId(), document.physicianId(), userId));
        auditService.logAccess(userId, AuditAction.DELETE_DOCUMENT, AuditResourceType.DOCUMENTO, documentId,
                document.patientId(), Map.of("fileName", document.fileName()));
        log.info("DocumentService: documento {} eliminado por {}", documentId, userId);
    }

    /** Contador de documentos activos del paciente (badge de notificaciones). */
    @Transactional(readOnly = true)
    public long activeDocumentCountForPatient(UUID patientId) {
        if (!properties.isEnabled() || patientId == null) {
            return 0;
        }
        return documentRepository.countActiveByPatientId(patientId);
    }

    private ClinicalDocument requireActiveDocument(UUID documentId) {
        return documentRepository
                .findById(documentId)
                .filter(ClinicalDocument::isActive)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    /**
     * Acceso a un documento activo respetando el ownership (ADR-036): el
     * paciente dueño, el médico asociado, quien lo subió o un ADMIN. Cualquier
     * otro usuario recibe 403 (sin filtrar existencia del documento).
     */
    @Transactional(readOnly = true)
    public ClinicalDocument requireAccessibleDocument(UUID documentId, UUID userId, boolean admin) {
        requireEnabled();
        ClinicalDocument document = requireActiveDocument(documentId);
        if (!admin && !isAccessible(document, userId)) {
            throw new DocumentAccessDeniedException("Acceso denegado: no tiene acceso a este documento");
        }
        return document;
    }

    /**
     * Texto extraído del documento para el análisis con IA. Si aún no se ha
     * extraído (subidas previas a la columna {@code extracted_text} o a la
     * caché), lo extrae bajo demanda del archivo almacenado y lo persiste para
     * reutilizarlo en los siguientes turnos de la conversación.
     */
    @Transactional
    public String ensureExtractedText(ClinicalDocument document) {
        requireEnabled();
        if (document.hasExtractedText()) {
            return document.extractedText();
        }
        if (textExtractor == null) {
            return "";
        }
        String text = "";
        try {
            text = textExtractor.extract(storage.load(document.storageKey()), document.fileName(), document.mimeType());
        } catch (RuntimeException e) {
            log.warn("DocumentService: no se pudo extraer el texto del documento {}: {}",
                    document.id(), e.getMessage());
            text = "";
        }
        if (!document.extractedText().equals(text)) {
            documentRepository.save(document.withExtractedText(text));
        }
        return text;
    }

    private void requireAccess(ClinicalDocument document, UUID userId, String message) {
        if (!isAccessible(document, userId)) {
            throw new DocumentAccessDeniedException(message);
        }
    }

    private boolean isAccessible(ClinicalDocument document, UUID userId) {
        boolean patient = document.patientId().equals(userId);
        boolean physician = document.physicianId() != null && document.physicianId().equals(userId);
        boolean uploader = document.uploadedBy().equals(userId);
        return patient || physician || uploader;
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "documento";
        }
        String cleaned = fileName.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
        if (cleaned.isBlank() || ".".equals(cleaned) || "..".equals(cleaned)) {
            return "documento";
        }
        return cleaned.length() > 100 ? cleaned.substring(cleaned.length() - 100) : cleaned;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new DocumentsDisabledException();
        }
    }

    private void publish(DomainEvent event) {
        if (outboxEventPublisher != null) {
            outboxEventPublisher.publish(event);
        } else if (eventBus != null) {
            eventBus.publish(event);
        }
    }
}
