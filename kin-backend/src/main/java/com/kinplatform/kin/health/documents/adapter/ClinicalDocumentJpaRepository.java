package com.kinplatform.kin.health.documents.adapter;

import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio JPA de documentos clínicos (ADR-036).
 */
public interface ClinicalDocumentJpaRepository extends JpaRepository<ClinicalDocumentEntity, UUID> {

    List<ClinicalDocumentEntity> findByPatientIdAndStatusOrderByUploadedAtDesc(UUID patientId, DocumentStatus status);

    List<ClinicalDocumentEntity> findByPhysicianIdAndStatusOrderByUploadedAtDesc(UUID physicianId, DocumentStatus status);

    List<ClinicalDocumentEntity> findByPatientIdAndPhysicianIdAndStatusOrderByUploadedAtDesc(
            UUID patientId, UUID physicianId, DocumentStatus status);

    long countByPatientIdAndStatus(UUID patientId, DocumentStatus status);

    @Query("SELECT COALESCE(SUM(e.fileSize), 0) FROM ClinicalDocumentEntity e WHERE e.patientId = :userId AND e.status = :status")
    long sumFileSizeByUserIdAndStatus(UUID userId, DocumentStatus status);
}
