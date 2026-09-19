package com.kinplatform.kin.health.documents.port;

import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de documentos clínicos (ADR-036).
 */
public interface ClinicalDocumentRepository {

    ClinicalDocument save(ClinicalDocument document);

    Optional<ClinicalDocument> findById(UUID id);

    /** Documentos visibles para el paciente (excluye HIDDEN_FROM_PATIENT y DELETED). */
    List<ClinicalDocument> findVisibleByPatientId(UUID patientId);

    /** Documentos activos de los pacientes de un médico (vista del médico). */
    List<ClinicalDocument> findActiveByPhysicianId(UUID physicianId);

    /** Documentos activos de un paciente creados por un médico (vista del médico por paciente). */
    List<ClinicalDocument> findActiveByPatientIdAndPhysicianId(UUID patientId, UUID physicianId);

    /** Contador de documentos visibles de un paciente (badge de notificaciones). */
    long countVisibleByPatientId(UUID patientId);
}
