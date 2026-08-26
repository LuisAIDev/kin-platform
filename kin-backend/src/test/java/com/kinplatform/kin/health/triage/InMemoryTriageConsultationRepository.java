package com.kinplatform.kin.health.triage;

import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Implementación en memoria del puerto {@link TriageConsultationRepository}
 * para tests (ADR-028).
 */
public class InMemoryTriageConsultationRepository implements TriageConsultationRepository {

    private final List<TriageConsultation> stored = new ArrayList<>();

    @Override
    public TriageConsultation save(TriageConsultation consultation) {
        stored.add(consultation);
        return consultation;
    }

    @Override
    public List<TriageConsultation> findByUserId(UUID userId) {
        return stored.stream().filter(c -> c.userId().equals(userId)).toList();
    }

    @Override
    public Page<TriageConsultation> findByUserId(UUID userId, Pageable pageable) {
        List<TriageConsultation> all = stored.stream()
                .filter(c -> c.userId().equals(userId))
                .sorted(Comparator.comparing(TriageConsultation::createdAt).reversed())
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<TriageConsultation> page = start > all.size() ? List.of() : all.subList(start, end);
        return new PageImpl<>(page, pageable, all.size());
    }

    @Override
    public Optional<TriageConsultation> findByIdAndUserId(UUID id, UUID userId) {
        return stored.stream()
                .filter(c -> c.id().equals(id) && c.userId().equals(userId))
                .findFirst();
    }

    public List<TriageConsultation> all() {
        return List.copyOf(stored);
    }
}
