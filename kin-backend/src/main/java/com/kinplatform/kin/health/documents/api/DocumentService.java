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
import com.kinplatform.kin.health.documents.infrastructure.DocumentStorage;
import com.kinplatform.kin.health.documents.port.ClinicalDocumentRepository;
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

    public DocumentService(
            ClinicalDocumentRepository documentRepository,
            DocumentStorage storage,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            DocumentProperties properties) {
        this(documentRepository, storage, accessValidator, auditService, properties, null, null);
    }

    @Autowired
    public DocumentService(
            ClinicalDocumentRepository documentRepository,
            DocumentStorage storage,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            DocumentProperties properties,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher) {
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.accessValidator = accessValidator;
        this.auditService = auditService;
        this.properties = properties;
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
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
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (content.length > properties.getMaxFileSize()) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido");
        }

        String safeName = sanitizeFileName(fileName);
        String storageKey = UUID.randomUUID() + "_" + safeName;
        storage.store(storageKey, content);

        ClinicalDocument document = ClinicalDocument.of(
                UUID.randomUUID(),
                safeName,
                content.length,
                mimeType,
                storageKey,
                physicianId,
                patientId,
                physicianId,
                description,
                DocumentStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        ClinicalDocument saved = documentRepository.save(document);
        publish(new DocumentUploadedEvent(saved.id(), patientId, physicianId, safeName));
        auditService.logAccess(physicianId, AuditAction.UPLOAD_DOCUMENT, AuditResourceType.DOCUMENTO, saved.id(),
                patientId, Map.of("fileName", safeName));
        log.info("DocumentService: documento {} subido por médico {} para paciente {}",
                saved.id(), physicianId, patientId);
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

    private void requireAccess(ClinicalDocument document, UUID userId, String message) {
        boolean patient = document.patientId().equals(userId);
        boolean physician = document.physicianId() != null && document.physicianId().equals(userId);
        boolean uploader = document.uploadedBy().equals(userId);
        if (!patient && !physician && !uploader) {
            throw new DocumentAccessDeniedException(message);
        }
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
