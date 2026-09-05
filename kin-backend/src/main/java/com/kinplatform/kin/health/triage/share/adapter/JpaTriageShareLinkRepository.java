package com.kinplatform.kin.health.triage.share.adapter;

import com.kinplatform.kin.health.triage.share.domain.TriageShareLink;
import com.kinplatform.kin.health.triage.share.port.TriageShareLinkRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link TriageShareLinkRepository}.
 */
@Component
public class JpaTriageShareLinkRepository implements TriageShareLinkRepository {

    private final TriageShareLinkJpaRepository repository;

    public JpaTriageShareLinkRepository(TriageShareLinkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TriageShareLink save(TriageShareLink link) {
        if (link == null) {
            throw new IllegalArgumentException("link no puede ser null");
        }
        var entity = repository.findById(link.id()).orElseGet(TriageShareLinkEntity::new);
        entity.setId(link.id());
        entity.setTriageId(link.triageId());
        entity.setPatientId(link.patientId());
        entity.setToken(link.token());
        entity.setExpiresAt(link.expiresAt());
        entity.setCreatedAt(link.createdAt());
        entity.setRevokedAt(link.revokedAt());
        entity.setCreatedBy(link.createdBy());
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TriageShareLink> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return repository.findByToken(token).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TriageShareLink> findActiveByTriageId(UUID triageId) {
        if (triageId == null) {
            return Optional.empty();
        }
        return repository.findFirstByTriageIdAndRevokedAtIsNullOrderByCreatedAtDesc(triageId).map(this::toDomain);
    }

    private TriageShareLink toDomain(TriageShareLinkEntity entity) {
        if (entity == null) {
            return null;
        }
        return TriageShareLink.of(
                entity.getId(),
                entity.getTriageId(),
                entity.getPatientId(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getRevokedAt(),
                entity.getCreatedBy());
    }
}
