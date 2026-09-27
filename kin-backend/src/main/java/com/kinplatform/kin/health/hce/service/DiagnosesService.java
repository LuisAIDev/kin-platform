package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.dto.DiagnosesResponse;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiagnosesService {

    private final DiagnosesRepository diagnosesRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public DiagnosesResponse addDiagnosis(CreateDiagnosisRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        if (request.getDiagnosisType() == Diagnoses.DiagnosisType.PRINCIPAL) {
            boolean hasPrincipal = diagnosesRepository.existsByEncounterIdAndDiagnosisTypeAndStatus(
                    encounter.getId(),
                    Diagnoses.DiagnosisType.PRINCIPAL,
                    Diagnoses.Status.ACTIVE
            );
            if (hasPrincipal) {
                throw new IllegalStateException("Encounter already has a principal diagnosis. Use setPrincipal to change it.");
            }
        }

        Diagnoses diagnosis = Diagnoses.builder()
                .encounterId(encounter.getId())
                .patientId(encounter.getPatientId())
                .physicianId(currentUser.getId())
                .cie10Code(request.getCie10Code())
                .cie10Description(request.getCie10Description())
                .diagnosisType(request.getDiagnosisType())
                .certainty(request.getCertainty() != null ? request.getCertainty() : Diagnoses.Certainty.CONFIRMED)
                .classification(request.getClassification() != null ? request.getClassification() : Diagnoses.Classification.CONSULTA)
                .supportedBy(request.getSupportedBy())
                .onsetDate(request.getOnsetDate())
                .resolutionDate(request.getResolutionDate())
                .status(request.getStatus() != null ? request.getStatus() : Diagnoses.Status.ACTIVE)
                .notes(request.getNotes())
                .build();

        Diagnoses saved = diagnosesRepository.saveAndFlush(diagnosis);
        return toResponse(saved);
    }

    @Transactional
    public DiagnosesResponse setPrincipal(UUID encounterId, UUID diagnosisId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        Diagnoses newPrincipal = diagnosesRepository.findById(diagnosisId)
                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found"));

        if (!newPrincipal.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Diagnosis does not belong to this encounter");
        }

        // Find and unmark current principal
        Optional<Diagnoses> currentPrincipal = diagnosesRepository.findByEncounterIdAndDiagnosisTypeAndStatus(
                encounterId,
                Diagnoses.DiagnosisType.PRINCIPAL,
                Diagnoses.Status.ACTIVE
        );

        if (currentPrincipal.isPresent() && !currentPrincipal.get().getId().equals(diagnosisId)) {
            Diagnoses oldPrincipal = currentPrincipal.get();
            oldPrincipal.setDiagnosisType(Diagnoses.DiagnosisType.SECUNDARIO);
            diagnosesRepository.saveAndFlush(oldPrincipal);
        }

        // Mark new principal
        newPrincipal.setDiagnosisType(Diagnoses.DiagnosisType.PRINCIPAL);
        Diagnoses saved = diagnosesRepository.saveAndFlush(newPrincipal);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DiagnosesResponse getByEncounter(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        List<Diagnoses> diagnoses = diagnosesRepository.findByEncounterIdOrderByCreatedAtDesc(encounterId);
        return DiagnosesResponse.builder()
                .build(); // Return list wrapper or just first? Let's return first for now
    }

    @Transactional(readOnly = true)
    public Optional<DiagnosesResponse> getPrincipal(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        return diagnosesRepository.findByEncounterIdAndDiagnosisTypeAndStatus(
                encounterId,
                Diagnoses.DiagnosisType.PRINCIPAL,
                Diagnoses.Status.ACTIVE
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DiagnosesResponse> getAllByEncounter(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        return diagnosesRepository.findByEncounterIdOrderByCreatedAtDesc(encounterId)
                .stream()
                .map(this::toResponse)
                .toList();
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

    private DiagnosesResponse toResponse(Diagnoses d) {
        return DiagnosesResponse.builder()
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
