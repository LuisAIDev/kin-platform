package com.kinplatform.kin.health.followup.adapter;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio JPA de tareas de seguimiento (ADR-033).
 */
public interface FollowUpTaskJpaRepository extends JpaRepository<FollowUpTaskEntity, UUID> {

    List<FollowUpTaskEntity> findByPlanIdOrderByDueDateAsc(UUID planId);

    List<FollowUpTaskEntity> findByPlanIdInOrderByDueDateAsc(Collection<UUID> planIds);

    @Query("select t from FollowUpTaskEntity t "
            + "where t.status = com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus.PENDING "
            + "and t.reminderSent = false and t.dueDate >= :from and t.dueDate <= :to")
    List<FollowUpTaskEntity> findPendingRemindableInWindow(
            @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("select t from FollowUpTaskEntity t "
            + "where t.status = com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus.PENDING "
            + "and t.dueDate < :now")
    List<FollowUpTaskEntity> findPendingDueBefore(@Param("now") OffsetDateTime now);

    @Query("select t from FollowUpTaskEntity t "
            + "where t.planId in :planIds "
            + "and t.status in (com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus.PENDING, "
            + "com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus.OVERDUE) "
            + "and t.dueDate < :now order by t.dueDate asc")
    List<FollowUpTaskEntity> findOpenDueBefore(
            @Param("planIds") Collection<UUID> planIds, @Param("now") OffsetDateTime now);

    @Query(
            "select count(t) from FollowUpTaskEntity t "
                    + "where t.planId in :planIds and t.status = com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus.PENDING")
    long countPendingByPlanIdIn(@Param("planIds") Collection<UUID> planIds);
}
