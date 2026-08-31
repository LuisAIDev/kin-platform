package com.kinplatform.kin.health.followup.port;

import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de tareas de seguimiento (ADR-033).
 */
public interface FollowUpTaskRepository {

    FollowUpTask save(FollowUpTask task);

    Optional<FollowUpTask> findById(UUID id);

    List<FollowUpTask> findByPlanId(UUID planId);

    List<FollowUpTask> findByPlanIdIn(Collection<UUID> planIds);

    /** Tareas PENDING (sin recordatorio enviado) con vencimiento en la ventana [from, to]. */
    List<FollowUpTask> findPendingRemindableInWindow(OffsetDateTime from, OffsetDateTime to);

    /** Tareas PENDING vencidas antes de {@code now} (scheduler de vencimiento). */
    List<FollowUpTask> findPendingDueBefore(OffsetDateTime now);

    /** Tareas PENDING/OVERDUE vencidas antes de {@code now} de un conjunto de planes (vista del médico). */
    List<FollowUpTask> findOpenDueBefore(Collection<UUID> planIds, OffsetDateTime now);

    /** Contador de tareas pendientes de un conjunto de planes (badge de notificaciones). */
    long countPendingByPlanIdIn(Collection<UUID> planIds);
}
