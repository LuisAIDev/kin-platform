package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreatePatientHistoryRequest;
import com.kinplatform.kin.health.hce.dto.PatientHistoryResponse;
import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Severity;
import com.kinplatform.kin.health.hce.repository.PatientHistoryRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
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
public class PatientHistoryService {

    private final PatientHistoryRepository patientHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public PatientHistoryResponse addHistory(CreatePatientHistoryRequest request) {
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        PatientHistory history = PatientHistory.builder()
                .patientId(request.getPatientId())
                .historyType(request.getHistoryType())
                .description(request.getDescription())
                .onsetDate(request.getOnsetDate())
                .resolutionDate(request.getResolutionDate())
                .status(request.getStatus() != null ? request.getStatus() : Status.ACTIVE)
                .severity(request.getSeverity())
                .notes(request.getNotes())
                .recordedBy(currentUser.getId())
                .recordedAt(Instant.now())
                .details(request.getDetails() != null ? request.getDetails() : "{}")
                .build();

        PatientHistory saved = patientHistoryRepository.saveAndFlush(history);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PatientHistoryResponse> getByPatientAndType(UUID patientId, HistoryType type) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return patientHistoryRepository.findByPatientIdAndHistoryType(patientId, type)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientHistoryResponse> getAllByPatient(UUID patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return patientHistoryRepository.findByPatientIdOrderByRecordedAtDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void checkPatientAccess(User patient) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = currentUser.getRole().equals(com.kinplatform.user.UserRole.PHYSICIAN);

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only physicians or admins can access patient history");
        }
    }

    private PatientHistoryResponse toResponse(PatientHistory h) {
        return PatientHistoryResponse.builder()
                .id(h.getId())
                .patientId(h.getPatientId())
                .historyType(h.getHistoryType())
                .description(h.getDescription())
                .onsetDate(h.getOnsetDate())
                .resolutionDate(h.getResolutionDate())
                .status(h.getStatus())
                .severity(h.getSeverity())
                .notes(h.getNotes())
                .recordedBy(h.getRecordedBy())
                .recordedAt(h.getRecordedAt())
                .details(h.getDetails())
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }
}
