package com.kinplatform.kin.health.audit;

import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditLog;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.audit.port.AuditLogRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Implementación en memoria del puerto de auditoría para tests (ADR-035).
 */
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final List<AuditLog> logs = new ArrayList<>();

    @Override
    public AuditLog save(AuditLog log) {
        logs.removeIf(l -> l.id().equals(log.id()));
        logs.add(log);
        return log;
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return logs.stream().filter(l -> l.id().equals(id)).findFirst();
    }

    @Override
    public Page<AuditLog> findByUserIdAndDateRange(UUID userId, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return page(logs.stream()
                .filter(l -> l.userId().equals(userId))
                .filter(l -> start == null || !l.timestamp().isBefore(start))
                .filter(l -> end == null || !l.timestamp().isAfter(end))
                .toList(), pageable);
    }

    @Override
    public Page<AuditLog> findByPatientIdAndDateRange(UUID patientId, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return page(logs.stream()
                .filter(l -> patientId.equals(l.patientId()))
                .filter(l -> start == null || !l.timestamp().isBefore(start))
                .filter(l -> end == null || !l.timestamp().isAfter(end))
                .toList(), pageable);
    }

    @Override
    public Page<AuditLog> findByActionAndResource(AuditAction action, AuditResourceType resourceType, UUID resourceId, Pageable pageable) {
        return page(logs.stream()
                .filter(l -> l.action() == action && l.resourceType() == resourceType)
                .filter(l -> resourceId == null || resourceId.equals(l.resourceId()))
                .toList(), pageable);
    }

    @Override
    public Page<AuditLog> search(UUID userId, UUID patientId, AuditAction action, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return page(logs.stream()
                .filter(l -> userId == null || userId.equals(l.userId()))
                .filter(l -> patientId == null || patientId.equals(l.patientId()))
                .filter(l -> action == null || l.action() == action)
                .filter(l -> start == null || !l.timestamp().isBefore(start))
                .filter(l -> end == null || !l.timestamp().isAfter(end))
                .toList(), pageable);
    }

    public List<AuditLog> all() {
        return List.copyOf(logs);
    }

    private Page<AuditLog> page(List<AuditLog> list, Pageable pageable) {
        list = list.stream()
                .sorted(Comparator.comparing(AuditLog::timestamp).reversed())
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<AuditLog> content = start > list.size() ? List.of() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }
}
