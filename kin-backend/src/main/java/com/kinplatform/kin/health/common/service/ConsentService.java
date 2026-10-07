package com.kinplatform.kin.health.common.service;

import com.kinplatform.kin.health.common.dto.CreateUserConsentRequest;
import com.kinplatform.kin.health.common.dto.RevokeConsentRequest;
import com.kinplatform.kin.health.common.dto.UserConsentResponse;
import com.kinplatform.kin.health.common.entity.UserConsent;
import com.kinplatform.kin.health.common.repository.UserConsentRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository userConsentRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserConsentResponse createOrUpdateConsent(CreateUserConsentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        checkSelfOrAdmin(user.getId());

        UserConsent.ConsentType consentType = UserConsent.ConsentType.valueOf(request.getConsentType().toUpperCase());

        // Check if a newer version exists and is active - if so, mark old version as obsolete
        if (request.getAccepted() && request.getVersion() != null) {
            markOlderVersionsAsObsolete(request.getUserId(), consentType, request.getVersion());
        }

        Optional<UserConsent> existing = userConsentRepository.findByUserIdAndConsentTypeAndVersion(
                request.getUserId(), consentType, request.getVersion());

        UserConsent consent;
        if (existing.isPresent()) {
            consent = existing.get();
            consent.setAccepted(request.getAccepted());
            consent.setAcceptedAt(request.getAccepted() ? Instant.now() : null);
            consent.setRevokedAt(!request.getAccepted() ? Instant.now() : null);
            consent.setIpAddress(request.getIpAddress());
            consent.setUserAgent(request.getUserAgent());
            if (request.getDocumentHash() != null) {
                consent.setDocumentHash(request.getDocumentHash());
            }
        } else {
            consent = UserConsent.builder()
                    .userId(request.getUserId())
                    .consentType(consentType)
                    .version(request.getVersion())
                    .accepted(request.getAccepted())
                    .acceptedAt(request.getAccepted() ? Instant.now() : null)
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .documentHash(request.getDocumentHash())
                    .build();
        }

        UserConsent saved = userConsentRepository.saveAndFlush(consent);
        return toResponse(saved);
    }

    /**
     * Marks older versions of a consent type as obsolete (accepted=false, revoked) 
     * when a newer version is accepted.
     */
    private void markOlderVersionsAsObsolete(UUID userId, UserConsent.ConsentType consentType, String newVersion) {
        List<UserConsent> olderVersions = userConsentRepository.findByUserIdAndConsentType(userId, consentType)
                .stream()
                .filter(c -> c.getVersion().compareTo(newVersion) < 0 && c.getAccepted())
                .toList();

        for (UserConsent oldConsent : olderVersions) {
            oldConsent.setAccepted(false);
            oldConsent.setRevokedAt(Instant.now());
            oldConsent.setAcceptedAt(null);
            oldConsent.setRevocationReason("Superseded by version " + newVersion);
        }
        if (!olderVersions.isEmpty()) {
            userConsentRepository.saveAllAndFlush(olderVersions);
        }
    }

    @Transactional(readOnly = true)
    public List<UserConsentResponse> getUserConsents(UUID userId) {
        checkSelfOrAdmin(userId);
        return userConsentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<UserConsentResponse> getActiveConsent(UUID userId, String consentType) {
        checkSelfOrAdmin(userId);
        UserConsent.ConsentType type = UserConsent.ConsentType.valueOf(consentType.toUpperCase());
        return userConsentRepository.findActiveConsent(userId, type)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<UserConsentResponse> getActiveConsent(UUID userId, String consentType, String version) {
        checkSelfOrAdmin(userId);
        UserConsent.ConsentType type = UserConsent.ConsentType.valueOf(consentType.toUpperCase());
        return userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, type, version)
                .map(this::toResponse);
    }

    /**
     * Check if user has active consent for a specific type and version.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveConsent(UUID userId, String consentType, String version) {
        UserConsent.ConsentType type = UserConsent.ConsentType.valueOf(consentType.toUpperCase());
        return userConsentRepository.findByUserIdAndConsentTypeAndVersion(userId, type, version)
                .map(c -> c.getAccepted() && c.getRevokedAt() == null)
                .orElse(false);
    }

    @Transactional
    public UserConsentResponse revokeConsent(RevokeConsentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        checkSelfOrAdmin(user.getId());

        UserConsent.ConsentType consentType = UserConsent.ConsentType.valueOf(request.getConsentType().toUpperCase());

        UserConsent consent = userConsentRepository.findByUserIdAndConsentTypeAndVersion(
                        request.getUserId(), consentType, request.getVersion())
                .orElseThrow(() -> new EntityNotFoundException("Consent not found"));

        consent.setAccepted(false);
        consent.setRevokedAt(Instant.now());
        consent.setAcceptedAt(null);
        if (request.getRevocationReason() != null) {
            consent.setRevocationReason(request.getRevocationReason());
        }

        UserConsent saved = userConsentRepository.saveAndFlush(consent);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveConsent(UUID userId, String consentType) {
        UserConsent.ConsentType type = UserConsent.ConsentType.valueOf(consentType.toUpperCase());
        return userConsentRepository.findActiveConsent(userId, type).isPresent();
    }

    private void checkSelfOrAdmin(UUID userId) {
        UUID currentUserId = getCurrentUserId();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        if (!currentUserId.equals(userId) && !isAdmin) {
            throw new AccessDeniedException("Not authorized to access this user's consents");
        }
    }

    private UUID getCurrentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private UserConsentResponse toResponse(UserConsent consent) {
        return UserConsentResponse.builder()
                .id(consent.getId())
                .userId(consent.getUserId())
                .consentType(consent.getConsentType().name())
                .version(consent.getVersion())
                .accepted(consent.getAccepted())
                .acceptedAt(consent.getAcceptedAt())
                .revokedAt(consent.getRevokedAt())
                .revocationReason(consent.getRevocationReason())
                .ipAddress(consent.getIpAddress())
                .userAgent(consent.getUserAgent())
                .documentHash(consent.getDocumentHash())
                .createdAt(consent.getCreatedAt())
                .updatedAt(consent.getUpdatedAt())
                .build();
    }
    
    /**
     * Generate SHA-256 hash of document content for integrity verification.
     */
    public static String generateDocumentHash(String documentContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(documentContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
