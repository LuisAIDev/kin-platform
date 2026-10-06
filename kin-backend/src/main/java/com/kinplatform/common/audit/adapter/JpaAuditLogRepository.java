package com.kinplatform.common.audit.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditLog;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.common.audit.port.AuditLogRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link AuditLogRepository} (ADR-035).
 */
@Component
public class JpaAuditLogRepository implements AuditLogRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaAuditLogRepository.class);

    private final AuditLogJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaAuditLogRepository(AuditLogJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        if (auditLog == null) {
            throw new IllegalArgumentException("auditLog no puede ser null");
        }
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(auditLog.id());
        entity.setUserId(auditLog.userId());
        entity.setAction(auditLog.action());
        entity.setResourceType(auditLog.resourceType());
        entity.setResourceId(auditLog.resourceId());
        entity.setPatientId(auditLog.patientId());
        entity.setTimestamp(auditLog.timestamp());
        entity.setIpAddress(auditLog.ipAddress());
        entity.setUserAgent(auditLog.userAgent());
        entity.setDetails(toJson(auditLog.details()));
        entity.setCreatedAt(OffsetDateTime.now());
        repository.save(entity);
        return auditLog;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLog> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByUserIdAndDateRange(
            UUID userId, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        if (start != null && end != null) {
            return repository
                    .findByUserIdAndTimestampGreaterThanEqualAndTimestampLessThanEqual(userId, start, end, pageable)
                    .map(this::toDomain);
        }
        return repository.findByUserIdOrderByTimestampDesc(userId, pageable).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByPatientIdAndDateRange(
            UUID patientId, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        if (start != null && end != null) {
            return repository
                    .findByPatientIdAndTimestampGreaterThanEqualAndTimestampLessThanEqual(patientId, start, end, pageable)
                    .map(this::toDomain);
        }
        return repository.findByPatientIdOrderByTimestampDesc(patientId, pageable).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByActionAndResource(
            AuditAction action, AuditResourceType resourceType, UUID resourceId, Pageable pageable) {
        if (resourceId != null) {
            return repository
                    .findByActionAndResourceTypeAndResourceIdOrderByTimestampDesc(action, resourceType, resourceId, pageable)
                    .map(this::toDomain);
        }
        return repository
                .findByActionAndResourceTypeOrderByTimestampDesc(action, resourceType, pageable)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(
            UUID userId, UUID patientId, AuditAction action, OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return repository.search(userId, patientId, action, start, end, pageable).map(this::toDomain);
    }

    private AuditLog toDomain(AuditLogEntity e) {
        return AuditLog.of(
                e.getId(),
                e.getUserId(),
                e.getAction(),
                e.getResourceType(),
                e.getResourceId(),
                e.getPatientId(),
                e.getTimestamp(),
                e.getIpAddress(),
                e.getUserAgent(),
                fromJson(e.getDetails()));
    }

    private String toJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception ex) {
            log.warn("JpaAuditLogRepository: no se pudo serializar details", ex);
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("JpaAuditLogRepository: no se pudo deserializar details", ex);
            return Map.of();
        }
    }
}


