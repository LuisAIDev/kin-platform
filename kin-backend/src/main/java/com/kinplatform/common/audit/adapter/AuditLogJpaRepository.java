package com.kinplatform.common.audit.adapter;

import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio JPA de auditoría (ADR-035).
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByUserIdAndTimestampGreaterThanEqualAndTimestampLessThanEqual(
            UUID userId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<AuditLogEntity> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    Page<AuditLogEntity> findByPatientIdAndTimestampGreaterThanEqualAndTimestampLessThanEqual(
            UUID patientId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<AuditLogEntity> findByPatientIdOrderByTimestampDesc(UUID patientId, Pageable pageable);

    Page<AuditLogEntity> findByActionAndResourceTypeAndResourceIdOrderByTimestampDesc(
            AuditAction action, AuditResourceType resourceType, UUID resourceId, Pageable pageable);

    Page<AuditLogEntity> findByActionAndResourceTypeOrderByTimestampDesc(
            AuditAction action, AuditResourceType resourceType, Pageable pageable);

    @Query("select l from AuditLogEntity l "
            + "where (:userId is null or l.userId = :userId) "
            + "and (:patientId is null or l.patientId = :patientId) "
            + "and (:action is null or l.action = :action) "
            + "and (:start is null or l.timestamp >= :start) "
            + "and (:end is null or l.timestamp <= :end) "
            + "order by l.timestamp desc")
    Page<AuditLogEntity> search(
            @Param("userId") UUID userId,
            @Param("patientId") UUID patientId,
            @Param("action") AuditAction action,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            Pageable pageable);
}


