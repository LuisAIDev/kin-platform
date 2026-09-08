package com.kinplatform.kin.health.documents.adapter;

import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.kin.health.documents.port.ClinicalDocumentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link ClinicalDocumentRepository} (ADR-036).
 */
@Component
public class JpaClinicalDocumentRepository implements ClinicalDocumentRepository {

    private final ClinicalDocumentJpaRepository repository;

    public JpaClinicalDocumentRepository(ClinicalDocumentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ClinicalDocument save(ClinicalDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("document no puede ser null");
        }
        ClinicalDocumentEntity entity = repository.findById(document.id()).orElseGet(ClinicalDocumentEntity::new);
        entity.setId(document.id());
        entity.setFileName(document.fileName());
        entity.setFileSize(document.fileSize());
        entity.setMimeType(document.mimeType());
        entity.setStorageKey(document.storageKey());
        entity.setUploadedBy(document.uploadedBy());
        entity.setPatientId(document.patientId());
        entity.setPhysicianId(document.physicianId());
        entity.setDescription(document.description());
        entity.setStatus(document.status());
        entity.setUploadedAt(document.uploadedAt());
        entity.setCreatedAt(document.createdAt());
        entity.setExtractedText(document.extractedText());
        repository.save(entity);
        return document;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClinicalDocument> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(JpaClinicalDocumentRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalDocument> findActiveByPatientId(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return repository.findByPatientIdAndStatusOrderByUploadedAtDesc(patientId, DocumentStatus.ACTIVE).stream()
                .map(JpaClinicalDocumentRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalDocument> findActiveByPhysicianId(UUID physicianId) {
        if (physicianId == null) {
            return List.of();
        }
        return repository.findByPhysicianIdAndStatusOrderByUploadedAtDesc(physicianId, DocumentStatus.ACTIVE).stream()
                .map(JpaClinicalDocumentRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalDocument> findActiveByPatientIdAndPhysicianId(UUID patientId, UUID physicianId) {
        if (patientId == null || physicianId == null) {
            return List.of();
        }
        return repository
                .findByPatientIdAndPhysicianIdAndStatusOrderByUploadedAtDesc(
                        patientId, physicianId, DocumentStatus.ACTIVE)
                .stream()
                .map(JpaClinicalDocumentRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByPatientId(UUID patientId) {
        if (patientId == null) {
            return 0;
        }
        return repository.countByPatientIdAndStatus(patientId, DocumentStatus.ACTIVE);
    }

    private static ClinicalDocument toDomain(ClinicalDocumentEntity e) {
        return ClinicalDocument.of(
                e.getId(),
                e.getFileName(),
                e.getFileSize(),
                e.getMimeType(),
                e.getStorageKey(),
                e.getUploadedBy(),
                e.getPatientId(),
                e.getPhysicianId(),
                e.getDescription(),
                e.getStatus(),
                e.getUploadedAt(),
                e.getCreatedAt(),
                e.getExtractedText());
    }
}
