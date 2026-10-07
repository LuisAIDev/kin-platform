package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateObstetricHistoryRequest;
import com.kinplatform.kin.health.hce.dto.ObstetricHistoryResponse;
import com.kinplatform.kin.health.hce.entity.ObstetricHistory;
import com.kinplatform.kin.health.hce.repository.ObstetricHistoryRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObstetricHistoryService {

    private final ObstetricHistoryRepository obstetricHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ObstetricHistoryResponse upsertHistory(CreateObstetricHistoryRequest request) {
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        // Validate clinical rule: gravida >= para + abortions + ectopicPregnancies + stillbirths
        Integer totalOutcomes = 0;
        if (request.getPara() != null) totalOutcomes += request.getPara();
        if (request.getAbortions() != null) totalOutcomes += request.getAbortions();
        if (request.getEctopicPregnancies() != null) totalOutcomes += request.getEctopicPregnancies();
        if (request.getStillbirths() != null) totalOutcomes += request.getStillbirths();

        if (request.getGravida() != null && request.getGravida() < totalOutcomes) {
            throw new IllegalArgumentException("Gravida cannot be less than sum of para + abortions + ectopicPregnancies + stillbirths");
        }

        // Check if existing record exists
        Optional<ObstetricHistory> existing = obstetricHistoryRepository.findByPatientId(request.getPatientId());
        ObstetricHistory history;

        if (existing.isPresent()) {
            history = existing.get();
            // Update existing
            if (request.getGravida() != null) history.setGravida(request.getGravida());
            if (request.getPara() != null) history.setPara(request.getPara());
            if (request.getAbortions() != null) history.setAbortions(request.getAbortions());
            if (request.getEctopicPregnancies() != null) history.setEctopicPregnancies(request.getEctopicPregnancies());
            if (request.getStillbirths() != null) history.setStillbirths(request.getStillbirths());
            if (request.getLivingChildren() != null) history.setLivingChildren(request.getLivingChildren());
            if (request.getCurrentPregnancy() != null) history.setCurrentPregnancy(request.getCurrentPregnancy());
            if (request.getLmp() != null) history.setLmp(request.getLmp());
            if (request.getEstimatedEdd() != null) history.setEstimatedEdd(request.getEstimatedEdd());
            if (request.getGestationalWeeks() != null) history.setGestationalWeeks(request.getGestationalWeeks());
            if (request.getPrenatalControls() != null) history.setPrenatalControls(request.getPrenatalControls());
            if (request.getPreviousDeliveries() != null) history.setPreviousDeliveries(request.getPreviousDeliveries());
            if (request.getBreastfeedingStatus() != null) history.setBreastfeedingStatus(request.getBreastfeedingStatus());
            if (request.getBreastfeedingDurationMonths() != null) history.setBreastfeedingDurationMonths(request.getBreastfeedingDurationMonths());
            if (request.getObstetricComplications() != null) history.setObstetricComplications(request.getObstetricComplications());
            history.setRecordedBy(currentUser.getId());
            history.setRecordedAt(Instant.now());
        } else {
            // Create new
            history = ObstetricHistory.builder()
                    .patientId(request.getPatientId())
                    .gravida(request.getGravida() != null ? request.getGravida() : 0)
                    .para(request.getPara() != null ? request.getPara() : 0)
                    .abortions(request.getAbortions() != null ? request.getAbortions() : 0)
                    .ectopicPregnancies(request.getEctopicPregnancies() != null ? request.getEctopicPregnancies() : 0)
                    .stillbirths(request.getStillbirths() != null ? request.getStillbirths() : 0)
                    .livingChildren(request.getLivingChildren() != null ? request.getLivingChildren() : 0)
                    .currentPregnancy(request.getCurrentPregnancy() != null ? request.getCurrentPregnancy() : false)
                    .lmp(request.getLmp())
                    .estimatedEdd(request.getEstimatedEdd())
                    .gestationalWeeks(request.getGestationalWeeks())
                    .prenatalControls(request.getPrenatalControls() != null ? request.getPrenatalControls() : 0)
                    .previousDeliveries(request.getPreviousDeliveries() != null ? request.getPreviousDeliveries() : "[]")
                    .breastfeedingStatus(request.getBreastfeedingStatus())
                    .breastfeedingDurationMonths(request.getBreastfeedingDurationMonths())
                    .obstetricComplications(request.getObstetricComplications() != null ? request.getObstetricComplications() : "[]")
                    .recordedBy(currentUser.getId())
                    .recordedAt(Instant.now())
                    .build();
        }

        ObstetricHistory saved = obstetricHistoryRepository.saveAndFlush(history);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<ObstetricHistoryResponse> getByPatientId(UUID patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return obstetricHistoryRepository.findByPatientId(patientId)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ObstetricHistoryResponse> getCurrentPregnancy(UUID patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return obstetricHistoryRepository.findCurrentPregnancyByPatientId(patientId)
                .map(this::toResponse);
    }

    private void checkPatientAccess(User patient) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = currentUser.getRole().equals(com.kinplatform.common.user.UserRole.PHYSICIAN);

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only physicians or admins can access patient obstetric history");
        }
    }

    private ObstetricHistoryResponse toResponse(ObstetricHistory h) {
        return ObstetricHistoryResponse.builder()
                .id(h.getId())
                .patientId(h.getPatientId())
                .gravida(h.getGravida())
                .para(h.getPara())
                .abortions(h.getAbortions())
                .ectopicPregnancies(h.getEctopicPregnancies())
                .stillbirths(h.getStillbirths())
                .livingChildren(h.getLivingChildren())
                .currentPregnancy(h.getCurrentPregnancy())
                .lmp(h.getLmp())
                .estimatedEdd(h.getEstimatedEdd())
                .gestationalWeeks(h.getGestationalWeeks())
                .prenatalControls(h.getPrenatalControls())
                .previousDeliveries(h.getPreviousDeliveries())
                .breastfeedingStatus(h.getBreastfeedingStatus())
                .breastfeedingDurationMonths(h.getBreastfeedingDurationMonths())
                .obstetricComplications(h.getObstetricComplications())
                .recordedBy(h.getRecordedBy())
                .recordedAt(h.getRecordedAt())
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }
}


