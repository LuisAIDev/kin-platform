package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateSurgicalHistoryRequest;
import com.kinplatform.kin.health.hce.dto.SurgicalHistoryResponse;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory;
import com.kinplatform.kin.health.hce.repository.SurgicalHistoryRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurgicalHistoryService {

    private final SurgicalHistoryRepository surgicalHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public SurgicalHistoryResponse addSurgery(CreateSurgicalHistoryRequest request) {
        com.kinplatform.common.user.User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        if (request.getSurgeryDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Surgery date cannot be in the future");
        }

        if (request.getProcedureCupsCode() == null || request.getProcedureCupsCode().isBlank()) {
            throw new IllegalArgumentException("Procedure CUPS code is required");
        }

        if (request.getAsaClassification() != null && (request.getAsaClassification() < 1 || request.getAsaClassification() > 6)) {
            throw new IllegalArgumentException("ASA classification must be between 1 and 6");
        }

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        SurgicalHistory surgery = SurgicalHistory.builder()
                .patientId(request.getPatientId())
                .surgeryDate(request.getSurgeryDate())
                .procedureCupsCode(request.getProcedureCupsCode())
                .procedureCupsDescription(request.getProcedureCupsDescription())
                .diagnosisCie10(request.getDiagnosisCie10())
                .diagnosisDescription(request.getDiagnosisDescription())
                .surgeryType(request.getSurgeryType())
                .anesthesiaType(request.getAnesthesiaType())
                .anesthesiologistId(request.getAnesthesiologistId())
                .asaClassification(request.getAsaClassification())
                .durationMinutes(request.getDurationMinutes())
                .estimatedBloodLossMl(request.getEstimatedBloodLossMl())
                .complications(request.getComplications() != null ? request.getComplications() : "[]")
                .surgeonId(request.getSurgeonId())
                .assistantSurgeonId(request.getAssistantSurgeonId())
                .institution(request.getInstitution())
                .notes(request.getNotes())
                .recordedBy(currentUser.getId())
                .recordedAt(Instant.now())
                .build();

        SurgicalHistory saved = surgicalHistoryRepository.saveAndFlush(surgery);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SurgicalHistoryResponse> getByPatient(UUID patientId) {
        com.kinplatform.common.user.User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return surgicalHistoryRepository.findByPatientIdOrderBySurgeryDateDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SurgicalHistoryResponse> getByCupsCode(String cupsCode) {
        return surgicalHistoryRepository.findByProcedureCupsCode(cupsCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void checkPatientAccess(com.kinplatform.common.user.User patient) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = currentUser.getRole().equals(com.kinplatform.common.user.UserRole.PHYSICIAN);

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only physicians or admins can access patient surgical history");
        }
    }

    private SurgicalHistoryResponse toResponse(SurgicalHistory s) {
        return SurgicalHistoryResponse.builder()
                .id(s.getId())
                .patientId(s.getPatientId())
                .surgeryDate(s.getSurgeryDate())
                .procedureCupsCode(s.getProcedureCupsCode())
                .procedureCupsDescription(s.getProcedureCupsDescription())
                .diagnosisCie10(s.getDiagnosisCie10())
                .diagnosisDescription(s.getDiagnosisDescription())
                .surgeryType(s.getSurgeryType())
                .anesthesiaType(s.getAnesthesiaType())
                .anesthesiologistId(s.getAnesthesiologistId())
                .asaClassification(s.getAsaClassification())
                .durationMinutes(s.getDurationMinutes())
                .estimatedBloodLossMl(s.getEstimatedBloodLossMl())
                .complications(s.getComplications())
                .surgeonId(s.getSurgeonId())
                .assistantSurgeonId(s.getAssistantSurgeonId())
                .institution(s.getInstitution())
                .notes(s.getNotes())
                .recordedBy(s.getRecordedBy())
                .recordedAt(s.getRecordedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}


