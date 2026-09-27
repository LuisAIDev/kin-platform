package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreatePhysicalExamRequest;
import com.kinplatform.kin.health.hce.dto.PhysicalExamResponse;
import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.PhysicalExamRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhysicalExamService {

    private final PhysicalExamRepository physicalExamRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public PhysicalExamResponse recordExam(CreatePhysicalExamRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        validateRanges(request);

        PhysicalExam exam = PhysicalExam.builder()
                .encounterId(encounter.getId())
                .evolutionId(request.getEvolutionId())
                .patientId(encounter.getPatientId())
                .physicianId(currentUser.getId())
                .bpSystolic(request.getBpSystolic())
                .bpDiastolic(request.getBpDiastolic())
                .heartRate(request.getHeartRate())
                .respiratoryRate(request.getRespiratoryRate())
                .temperature(request.getTemperature())
                .spo2(request.getSpo2())
                .weightKg(request.getWeightKg())
                .heightCm(request.getHeightCm())
                .glasgowScore(request.getGlasgowScore())
                .painScale(request.getPainScale())
                .painScaleType(request.getPainScaleType() != null ? request.getPainScaleType() : PhysicalExam.PainScaleType.EVA)
                .generalAppearance(request.getGeneralAppearance())
                .headNeck(request.getHeadNeck())
                .cardiovascular(request.getCardiovascular())
                .respiratory(request.getRespiratory())
                .abdominal(request.getAbdominal())
                .neurological(request.getNeurological())
                .musculoskeletal(request.getMusculoskeletal())
                .skin(request.getSkin())
                .genitourinary(request.getGenitourinary())
                .psychiatric(request.getPsychiatric())
                .validatedScales(request.getValidatedScales() != null ? request.getValidatedScales() : "{}")
                .build();

        // BMI is calculated by DB generated column, but we can compute it for response
        PhysicalExam saved = physicalExamRepository.saveAndFlush(exam);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PhysicalExamResponse getByEncounter(UUID encounterId) {
        PhysicalExam exam = physicalExamRepository.findByEncounterId(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Physical exam not found for encounter"));

        checkAccessByExam(exam);
        return toResponse(exam);
    }

    @Transactional(readOnly = true)
    public PhysicalExamResponse getLatestByPatient(UUID patientId) {
        List<PhysicalExam> exams = physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
        if (exams.isEmpty()) {
            throw new EntityNotFoundException("No physical exams found for patient");
        }

        PhysicalExam latest = exams.get(0);
        checkAccessByExam(latest);
        return toResponse(latest);
    }

    private void validateRanges(CreatePhysicalExamRequest request) {
        if (request.getBpSystolic() != null && (request.getBpSystolic() < 50 || request.getBpSystolic() > 300)) {
            throw new IllegalArgumentException("Systolic BP must be between 50 and 300");
        }
        if (request.getHeartRate() != null && (request.getHeartRate() < 30 || request.getHeartRate() > 250)) {
            throw new IllegalArgumentException("Heart rate must be between 30 and 250");
        }
        if (request.getTemperature() != null && (request.getTemperature().compareTo(BigDecimal.valueOf(30)) < 0 || request.getTemperature().compareTo(BigDecimal.valueOf(45)) > 0)) {
            throw new IllegalArgumentException("Temperature must be between 30.0 and 45.0");
        }
        if (request.getSpo2() != null && (request.getSpo2() < 50 || request.getSpo2() > 100)) {
            throw new IllegalArgumentException("SpO2 must be between 50 and 100");
        }
        if (request.getGlasgowScore() != null && (request.getGlasgowScore() < 3 || request.getGlasgowScore() > 15)) {
            throw new IllegalArgumentException("Glasgow score must be between 3 and 15");
        }
        if (request.getPainScale() != null && (request.getPainScale() < 0 || request.getPainScale() > 10)) {
            throw new IllegalArgumentException("Pain scale must be between 0 and 10");
        }
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

    private void checkAccessByExam(PhysicalExam exam) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = exam.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this physical exam");
        }
    }

    private PhysicalExamResponse toResponse(PhysicalExam p) {
        PhysicalExamResponse response = PhysicalExamResponse.builder()
                .id(p.getId())
                .encounterId(p.getEncounterId())
                .evolutionId(p.getEvolutionId())
                .patientId(p.getPatientId())
                .physicianId(p.getPhysicianId())
                .recordedAt(p.getRecordedAt())
                .bpSystolic(p.getBpSystolic())
                .bpDiastolic(p.getBpDiastolic())
                .heartRate(p.getHeartRate())
                .respiratoryRate(p.getRespiratoryRate())
                .temperature(p.getTemperature())
                .spo2(p.getSpo2())
                .weightKg(p.getWeightKg())
                .heightCm(p.getHeightCm())
                .bmi(p.getBmi())
                .glasgowScore(p.getGlasgowScore())
                .painScale(p.getPainScale())
                .painScaleType(p.getPainScaleType())
                .generalAppearance(p.getGeneralAppearance())
                .headNeck(p.getHeadNeck())
                .cardiovascular(p.getCardiovascular())
                .respiratory(p.getRespiratory())
                .abdominal(p.getAbdominal())
                .neurological(p.getNeurological())
                .musculoskeletal(p.getMusculoskeletal())
                .skin(p.getSkin())
                .genitourinary(p.getGenitourinary())
                .psychiatric(p.getPsychiatric())
                .validatedScales(p.getValidatedScales())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();

        // Compute BMI if weight and height are available and BMI not set by DB
        if (p.getWeightKg() != null && p.getHeightCm() != null) {
            BigDecimal heightM = p.getHeightCm().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal bmi = p.getWeightKg().divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
            response.setBmi(bmi);
        }

        return response;
    }
}
