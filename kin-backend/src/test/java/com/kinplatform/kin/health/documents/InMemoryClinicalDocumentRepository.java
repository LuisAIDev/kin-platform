package com.kinplatform.kin.health.documents;

import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.kin.health.documents.port.ClinicalDocumentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del puerto de documentos clínicos para tests (ADR-036).
 */
public class InMemoryClinicalDocumentRepository implements ClinicalDocumentRepository {

    private final java.util.Map<UUID, ClinicalDocument> documents = new ConcurrentHashMap<>();

    @Override
    public ClinicalDocument save(ClinicalDocument document) {
        documents.put(document.id(), document);
        return document;
    }

    @Override
    public Optional<ClinicalDocument> findById(UUID id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public List<ClinicalDocument> findVisibleByPatientId(UUID patientId) {
        return documents.values().stream()
                .filter(d -> d.patientId().equals(patientId) && d.isVisibleToPatient())
                .sorted(Comparator.comparing(ClinicalDocument::uploadedAt).reversed())
                .toList();
    }

    @Override
    public List<ClinicalDocument> findActiveByPhysicianId(UUID physicianId) {
        return documents.values().stream()
                .filter(d -> physicianId.equals(d.physicianId()) && d.isActive())
                .sorted(Comparator.comparing(ClinicalDocument::uploadedAt).reversed())
                .toList();
    }

    @Override
    public List<ClinicalDocument> findActiveByPatientIdAndPhysicianId(UUID patientId, UUID physicianId) {
        return documents.values().stream()
                .filter(d -> d.patientId().equals(patientId) && physicianId.equals(d.physicianId()) && d.isActive())
                .sorted(Comparator.comparing(ClinicalDocument::uploadedAt).reversed())
                .toList();
    }

    @Override
    public long countVisibleByPatientId(UUID patientId) {
        return documents.values().stream()
                .filter(d -> d.patientId().equals(patientId) && d.isVisibleToPatient())
                .count();
    }

    public List<ClinicalDocument> all() {
        return List.copyOf(documents.values());
    }
}
