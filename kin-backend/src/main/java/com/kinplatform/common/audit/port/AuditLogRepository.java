package com.kinplatform.common.audit.port;

import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditLog;
import com.kinplatform.common.audit.domain.AuditResourceType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de persistencia de la auditoría de accesos a datos de salud (ADR-035).
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog log);

    Optional<AuditLog> findById(UUID id);

    /** Logs de un usuario en un rango de fechas (o todo si los límites son nulos). */
    Page<AuditLog> findByUserIdAndDateRange(UUID userId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    /** Logs de accesos a los datos de un paciente. */
    Page<AuditLog> findByPatientIdAndDateRange(UUID patientId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    /** Logs por acción y tipo de recurso (y opcionalmente recurso concreto). */
    Page<AuditLog> findByActionAndResource(
            AuditAction action, AuditResourceType resourceType, UUID resourceId, Pageable pageable);

    /** Logs con filtros combinados (ADMIN). */
    Page<AuditLog> search(
            UUID userId, UUID patientId, AuditAction action, OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}


