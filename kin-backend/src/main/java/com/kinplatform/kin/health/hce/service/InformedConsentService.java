package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateInformedConsentRequest;
import com.kinplatform.kin.health.hce.dto.InformedConsentResponse;
import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.kin.health.hce.repository.InformedConsentRepository;
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
public class InformedConsentService {

    private final InformedConsentRepository informedConsentRepository;
    private final UserRepository userRepository;

    @Transactional
    public InformedConsentResponse createConsent(CreateInformedConsentRequest request) {
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        InformedConsent consent = InformedConsent.builder()
                .patientId(request.getPatientId())
                .procedureName(request.getProcedureName())
                .procedureCupsCode(request.getProcedureCupsCode())
                .consentType(request.getConsentType())
                .documentVersion(request.getDocumentVersion())
                .documentStorageKey(request.getDocumentStorageKey())
                .patientSignatureHash(request.getPatientSignatureHash())
                .witness1Name(request.getWitness1Name())
                .witness1Document(request.getWitness1Document())
                .witness1SignatureHash(request.getWitness1SignatureHash())
                .witness2Name(request.getWitness2Name())
                .witness2Document(request.getWitness2Document())
                .witness2SignatureHash(request.getWitness2SignatureHash())
                .physicianId(currentUser.getId())
                .physicianSignatureHash(request.getPhysicianSignatureHash())
                .signedAt(Instant.now())
                .status(InformedConsent.Status.VALID)
                .build();

        InformedConsent saved = informedConsentRepository.saveAndFlush(consent);
        return toResponse(saved);
    }

    @Transactional
    public InformedConsentResponse revokeConsent(UUID consentId, String reason) {
        InformedConsent consent = informedConsentRepository.findById(consentId)
                .orElseThrow(() -> new EntityNotFoundException("Informed consent not found"));

        checkAccessByConsent(consent);

        if (consent.getStatus() == InformedConsent.Status.REVOKED) {
            throw new IllegalStateException("Consent already revoked");
        }
        if (consent.getStatus() == InformedConsent.Status.EXPIRED) {
            throw new IllegalStateException("Cannot revoke expired consent");
        }

        consent.setStatus(InformedConsent.Status.REVOKED);
        consent.setRevokedAt(Instant.now());
        consent.setRevocationReason(reason != null ? reason : "Revoked without reason");

        InformedConsent saved = informedConsentRepository.saveAndFlush(consent);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InformedConsentResponse> getByPatient(UUID patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        checkPatientAccess(patient);

        return informedConsentRepository.findByPatientIdOrderBySignedAtDesc(patientId)
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
            throw new AccessDeniedException("Only physicians or admins can access patient consents");
        }
    }

    private void checkAccessByConsent(InformedConsent consent) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = consent.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this consent");
        }
    }

    private InformedConsentResponse toResponse(InformedConsent c) {
        return InformedConsentResponse.builder()
                .id(c.getId())
                .patientId(c.getPatientId())
                .procedureName(c.getProcedureName())
                .procedureCupsCode(c.getProcedureCupsCode())
                .consentType(c.getConsentType())
                .documentVersion(c.getDocumentVersion())
                .documentStorageKey(c.getDocumentStorageKey())
                .patientSignatureHash(c.getPatientSignatureHash())
                .witness1Name(c.getWitness1Name())
                .witness1Document(c.getWitness1Document())
                .witness1SignatureHash(c.getWitness1SignatureHash())
                .witness2Name(c.getWitness2Name())
                .witness2Document(c.getWitness2Document())
                .witness2SignatureHash(c.getWitness2SignatureHash())
                .physicianId(c.getPhysicianId())
                .physicianSignatureHash(c.getPhysicianSignatureHash())
                .signedAt(c.getSignedAt())
                .expiresAt(c.getExpiresAt())
                .revokedAt(c.getRevokedAt())
                .revocationReason(c.getRevocationReason())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}