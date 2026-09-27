package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CounterReferralRequest;
import com.kinplatform.kin.health.hce.dto.CreateReferralRequest;
import com.kinplatform.kin.health.hce.dto.ReferralResponse;
import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.repository.ReferralRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReferralResponse createReferral(CreateReferralRequest request) {
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        Referral referral = Referral.builder()
                .patientId(request.getPatientId())
                .referringPhysicianId(currentUser.getId())
                .referringService(request.getReferringService())
                .referredToService(request.getReferredToService())
                .referredToInstitution(request.getReferredToInstitution())
                .referredToPhysicianId(request.getReferredToPhysicianId())
                .referralType(request.getReferralType())
                .priority(request.getPriority() != null ? request.getPriority() : Referral.Priority.ROUTINE)
                .reason(request.getReason())
                .clinicalSummary(request.getClinicalSummary())
                .status(Referral.Status.PENDING)
                .scheduledAt(request.getScheduledAt())
                .build();

        Referral saved = referralRepository.saveAndFlush(referral);
        return toResponse(saved);
    }

    @Transactional
    public ReferralResponse counterReferral(UUID referralId, CounterReferralRequest request) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new EntityNotFoundException("Referral not found"));

        checkAccessByReferral(referral);

        if (referral.getStatus() == Referral.Status.COMPLETED) {
            throw new IllegalStateException("Referral already counter-referred");
        }
        if (referral.getStatus() == Referral.Status.CANCELLED) {
            throw new IllegalStateException("Cannot counter-refer cancelled referral");
        }
        if (referral.getStatus() == Referral.Status.REJECTED) {
            throw new IllegalStateException("Cannot counter-refer rejected referral");
        }

        referral.setStatus(Referral.Status.COMPLETED);
        referral.setCounterreferralSummary(request.getCounterreferralSummary());
        referral.setCounterreferralRecommendations(request.getCounterreferralRecommendations());
        referral.setCounterreferralAt(Instant.now());
        referral.setCounterreferralBy(request.getCounterreferralBy());
        referral.setCompletedAt(Instant.now());

        Referral saved = referralRepository.saveAndFlush(referral);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getByPatient(UUID patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return referralRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> getByStatus(Referral.Status status) {
        return referralRepository.findByStatus(status)
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
            throw new AccessDeniedException("Only physicians or admins can access patient referrals");
        }
    }

    private void checkAccessByReferral(Referral referral) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = referral.getReferringPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this referral");
        }
    }

    private ReferralResponse toResponse(Referral r) {
        return ReferralResponse.builder()
                .id(r.getId())
                .patientId(r.getPatientId())
                .referringPhysicianId(r.getReferringPhysicianId())
                .referringService(r.getReferringService())
                .referredToService(r.getReferredToService())
                .referredToInstitution(r.getReferredToInstitution())
                .referredToPhysicianId(r.getReferredToPhysicianId())
                .referralType(r.getReferralType())
                .priority(r.getPriority())
                .reason(r.getReason())
                .clinicalSummary(r.getClinicalSummary())
                .status(r.getStatus())
                .counterreferralSummary(r.getCounterreferralSummary())
                .counterreferralRecommendations(r.getCounterreferralRecommendations())
                .counterreferralAt(r.getCounterreferralAt())
                .counterreferralBy(r.getCounterreferralBy())
                .scheduledAt(r.getScheduledAt())
                .completedAt(r.getCompletedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
