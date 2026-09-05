package com.kinplatform.kin.health.triage.share;

import com.kinplatform.kin.health.triage.share.domain.TriageShareLink;
import com.kinplatform.kin.health.triage.share.port.TriageShareLinkRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositorio en memoria para tests del módulo de compartición de triaje. */
public class InMemoryTriageShareLinkRepository implements TriageShareLinkRepository {

    private final List<TriageShareLink> links = new ArrayList<>();

    @Override
    public TriageShareLink save(TriageShareLink link) {
        links.removeIf(l -> l.id().equals(link.id()));
        links.add(link);
        return link;
    }

    @Override
    public Optional<TriageShareLink> findByToken(String token) {
        return links.stream().filter(l -> l.token().equals(token)).findFirst();
    }

    @Override
    public Optional<TriageShareLink> findActiveByTriageId(UUID triageId) {
        return links.stream()
                .filter(l -> l.triageId().equals(triageId) && l.revokedAt() == null)
                .findFirst();
    }
}
