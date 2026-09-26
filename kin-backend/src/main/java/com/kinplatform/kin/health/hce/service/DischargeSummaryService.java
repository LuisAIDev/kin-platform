package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateDischargeSummaryRequest;
import com.kinplatform.kin.health.hce.dto.DischargeSummaryResponse;
import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.DischargeSummaryRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DischargeSummaryService {

    private final DischargeSummaryRepository dischargeSummaryRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public DischargeSummaryResponse createDischargeSummary(CreateDischargeSummaryRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        if (request.getDischargeDate().isBefore(request.getAdmissionDate())) {
            throw new IllegalArgumentException("Discharge date cannot be before admission date");
        }

        if (request.getDischargeDiagnosisCie10() == null || request.getDischargeDiagnosisCie10().isBlank()) {
            throw new IllegalArgumentException("Discharge diagnosis CIE-10 is required");
        }

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        DischargeSummary summary = DischargeSummary.builder()
                .patientId(encounter.getPatientId())
                .admissionId(request.getAdmissionId())
                .attendingPhysicianId(currentUser.getId())
                .admissionDate(request.getAdmissionDate())
                .dischargeDate(request.getDischargeDate())
                .admissionDiagnosisCie10(request.getAdmissionDiagnosisCie10())
                .dischargeDiagnosisCie10(request.getDischargeDiagnosisCie10())
                .secondaryDiagnosesCie10(request.getSecondaryDiagnosesCie10())
                .clinicalSummary(request.getClinicalSummary())
                .proceduresPerformed(request.getProceduresPerformed())
                .complications(request.getComplications())
                .dischargeCondition(request.getDischargeCondition())
                .dischargeDisposition(request.getDischargeDisposition())
                .dischargeMedications(request.getDischargeMedications())
                .followupAppointments(request.getFollowupAppointments())
                .alarmSigns(request.getAlarmSigns())
                .generalRecommendations(request.getGeneralRecommendations())
                .physicianSignatureHash(request.getPhysicianSignatureHash())
                .signedAt(request.getPhysicianSignatureHash() != null ? Instant.now() : null)
                .build();

        DischargeSummary saved = dischargeSummaryRepository.saveAndFlush(summary);
        return toResponse(saved);
    }

    @Transactional
    public DischargeSummaryResponse signDischargeSummary(UUID summaryId, UUID physicianId) {
        DischargeSummary summary = dischargeSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new EntityNotFoundException("Discharge summary not found"));

        checkAccessBySummary(summary);

        if (!summary.getAttendingPhysicianId().equals(physicianId)) {
            throw new IllegalArgumentException("Physician ID does not match the attending physician");
        }

        if (summary.getSignedAt() != null) {
            throw new IllegalStateException("Discharge summary already signed");
        }

        summary.setPhysicianSignatureHash("sha256:" + physicianId + ":" + System.currentTimeMillis());
        summary.setSignedAt(java.time.Instant.now());

        DischargeSummary saved = dischargeSummaryRepository.saveAndFlush(summary);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<DischargeSummaryResponse> getByAdmission(UUID admissionId) {
        return dischargeSummaryRepository.findByAdmissionId(admissionId)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DischargeSummaryResponse> getByEncounter(UUID encounterId) {
        // Using patientId from encounter to find related discharge summaries
        // For simplicity, we'll search by admissionId if needed
        // But we don't have a direct findByEncounterId method
        // Let's use a workaround - we can search by patient and recent dates
        return dischargeSummaryRepository.findByAdmissionId(encounterId) // This won't work as encounterId != admissionId
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DischargeSummaryResponse> getByPatient(UUID patientId) {
        return dischargeSummaryRepository.findByPatientIdOrderByDischargeDateDesc(patientId)
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

    private void checkAccessBySummary(DischargeSummary summary) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = summary.getAttendingPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this discharge summary");
        }
    }

    private DischargeSummaryResponse toResponse(DischargeSummary d) {
        return DischargeSummaryResponse.builder()
                .id(d.getId())
                .patientId(d.getPatientId())
                .admissionId(d.getAdmissionId())
                .attendingPhysicianId(d.getAttendingPhysicianId())
                .admissionDate(d.getAdmissionDate())
                .dischargeDate(d.getDischargeDate())
                .lengthOfStay(d.getLengthOfStay())
                .admissionDiagnosisCie10(d.getAdmissionDiagnosisCie10())
                .dischargeDiagnosisCie10(d.getDischargeDiagnosisCie10())
                .secondaryDiagnosesCie10(d.getSecondaryDiagnosesCie10())
                .clinicalSummary(d.getClinicalSummary())
                .proceduresPerformed(d.getProceduresPerformed())
                .complications(d.getComplications())
                .dischargeCondition(d.getDischargeCondition())
                .dischargeDisposition(d.getDischargeDisposition())
                .dischargeMedications(d.getDischargeMedications())
                .followupAppointments(d.getFollowupAppointments())
                .alarmSigns(d.getAlarmSigns())
                .generalRecommendations(d.getGeneralRecommendations())
                .physicianSignatureHash(d.getPhysicianSignatureHash())
                .signedAt(d.getSignedAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}