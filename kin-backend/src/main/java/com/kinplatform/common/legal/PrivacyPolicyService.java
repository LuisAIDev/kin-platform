package com.kinplatform.common.legal;

import com.kinplatform.common.legal.PrivacyPolicyVersion;
import com.kinplatform.common.legal.PrivacyPolicyVersionRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyPolicyService {

    private final PrivacyPolicyVersionRepository policyRepository;
    private final UserRepository userRepository;
    private final AuditLogJpaRepository auditRepository;

    public Optional<PrivacyPolicyVersion> getActivePolicy() {
        return policyRepository.findByActiveTrue();
    }

    public Optional<PrivacyPolicyVersion> getPolicyByVersion(String version) {
        return policyRepository.findByVersion(version);
    }

    public List<PrivacyPolicyVersion> listAllVersions() {
        return policyRepository.findAllByOrderByEffectiveDateDesc();
    }

    @Transactional
    public PrivacyPolicyVersion publishNewVersion(String version, String title, String contentMd, java.time.LocalDate effectiveDate, UUID adminId) {
        // Check if version already exists
        if (policyRepository.findByVersion(version).isPresent()) {
            throw new IllegalArgumentException("La versión " + version + " ya existe");
        }

        // Deactivate current active version
        policyRepository.findByActiveTrue().ifPresent(active -> {
            active.setActive(false);
            policyRepository.save(active);
        });

        // Get admin user
        User admin = userRepository.findById(adminId).orElseThrow();

        // Create new version
        PrivacyPolicyVersion newVersion = PrivacyPolicyVersion.builder()
            .version(version)
            .title(title)
            .contentMd(contentMd)
            .effectiveDate(effectiveDate)
            .publishedAt(Instant.now())
            .publishedBy(adminId)
            .active(true)
            .build();

        PrivacyPolicyVersion saved = policyRepository.save(newVersion);

        // Audit
        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            java.util.UUID.randomUUID(),
            adminId,
            com.kinplatform.common.audit.domain.AuditAction.UPDATE,
            com.kinplatform.common.audit.domain.AuditResourceType.USER,
            saved.getId(),
            saved.getId(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            java.util.Map.of(
                "version", saved.getVersion(),
                "title", saved.getTitle(),
                "effectiveDate", saved.getEffectiveDate().toString(),
                "action", "PRIVACY_POLICY_PUBLISHED"
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return saved;
    }

    public List<PrivacyPolicyVersion> getPolicyHistory() {
        return policyRepository.findAllByOrderByEffectiveDateDesc();
    }
}



