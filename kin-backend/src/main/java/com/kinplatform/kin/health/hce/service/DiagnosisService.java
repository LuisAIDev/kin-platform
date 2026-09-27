package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.DiagnosisResponse;
import com.kinplatform.kin.health.hce.dto.request.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.entity.Diagnosis;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.DiagnosisRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public DiagnosisResponse createDiagnosis(CreateDiagnosisRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        // Parse String fields to enums
        Diagnosis.DiagnosisType diagnosisType = Diagnosis.DiagnosisType.valueOf(request.getDiagnosisType());
        Diagnosis.Certainty certainty = Diagnosis.Certainty.valueOf(request.getCertainty());
        Diagnosis.Classification classification = request.getClassification() != null
                ? Diagnosis.Classification.valueOf(request.getClassification())
                : Diagnosis.Classification.CONSULTA;
        Diagnosis.Status status = request.getStatus() != null
                ? Diagnosis.Status.valueOf(request.getStatus())
                : Diagnosis.Status.ACTIVE;

        // Check if trying to create a PRINCIPAL diagnosis when one already exists
        if (diagnosisType == Diagnosis.DiagnosisType.PRINCIPAL) {
            long principalCount = diagnosisRepository.countByEncounterIdAndType(encounter.getId(), Diagnosis.DiagnosisType.PRINCIPAL);
            if (principalCount > 0) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "A principal diagnosis already exists for this encounter. Use setPrincipal endpoint to change it.");
            }
        }

        Diagnosis diagnosis = Diagnosis.builder()
                .encounterId(encounter.getId())
                .patientId(encounter.getPatientId())
                .physicianId(currentUser.getId())
                .cie10Code(request.getCie10Code())
                .cie10Description(request.getCie10Description())
                .diagnosisType(diagnosisType)
                .certainty(certainty)
                .classification(classification)
                .supportedBy(request.getSupportedBy())
                .onsetDate(request.getOnsetDate())
                .resolutionDate(request.getResolutionDate())
                .status(status)
                .notes(request.getNotes())
                .build();

        Diagnosis saved = diagnosisRepository.saveAndFlush(diagnosis);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisResponse> getByEncounter(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        return diagnosisRepository.findByEncounterId(encounterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DiagnosisResponse setPrincipal(UUID encounterId, UUID diagnosisId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found"));

        // Verify the diagnosis belongs to the encounter
        if (!diagnosis.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Diagnosis does not belong to the specified encounter");
        }

        // If already principal, no-op
        if (diagnosis.getDiagnosisType() == Diagnosis.DiagnosisType.PRINCIPAL) {
            return toResponse(diagnosis);
        }

        // Demote existing principal to SECUNDARIO
        int demoted = diagnosisRepository.updateDiagnosisTypeByEncounter(encounterId,
                Diagnosis.DiagnosisType.PRINCIPAL,
                Diagnosis.DiagnosisType.SECUNDARIO);

        // Promote this diagnosis to PRINCIPAL
        diagnosisRepository.setDiagnosisType(diagnosisId, Diagnosis.DiagnosisType.PRINCIPAL);

        // Refresh
        Diagnosis updated = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found after update"));
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public DiagnosisResponse getPrincipal(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Encounter not found"));

        checkAccess(encounter);

        return diagnosisRepository.findByEncounterIdAndDiagnosisType(encounterId, Diagnosis.DiagnosisType.PRINCIPAL)
                .map(this::toResponse)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "No principal diagnosis found for this encounter"));
    }

    private void checkAccess(Encounter encounter) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = encounter.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this encounter");
        }
    }

    private DiagnosisResponse toResponse(Diagnosis d) {
        return DiagnosisResponse.builder()
                .id(d.getId())
                .encounterId(d.getEncounterId())
                .patientId(d.getPatientId())
                .physicianId(d.getPhysicianId())
                .cie10Code(d.getCie10Code())
                .cie10Description(d.getCie10Description())
                .diagnosisType(d.getDiagnosisType())
                .certainty(d.getCertainty())
                .classification(d.getClassification())
                .supportedBy(d.getSupportedBy())
                .onsetDate(d.getOnsetDate())
                .resolutionDate(d.getResolutionDate())
                .status(d.getStatus())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}