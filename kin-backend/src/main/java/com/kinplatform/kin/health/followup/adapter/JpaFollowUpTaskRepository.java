package com.kinplatform.kin.health.followup.adapter;

import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import com.kinplatform.kin.health.followup.domain.FollowUpTaskStatus;
import com.kinplatform.kin.health.followup.port.FollowUpTaskRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link FollowUpTaskRepository} (ADR-033).
 */
@Component
public class JpaFollowUpTaskRepository implements FollowUpTaskRepository {

    private final FollowUpTaskJpaRepository repository;

    public JpaFollowUpTaskRepository(FollowUpTaskJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public FollowUpTask save(FollowUpTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task no puede ser null");
        }
        FollowUpTaskEntity entity = repository.findById(task.id()).orElseGet(FollowUpTaskEntity::new);
        entity.setId(task.id());
        entity.setPlanId(task.planId());
        entity.setDescription(task.description());
        entity.setDueDate(task.dueDate());
        entity.setStatus(task.status());
        entity.setCompletedAt(task.completedAt());
        entity.setReminderSent(task.reminderSent());
        entity.setCreatedAt(task.createdAt());
        repository.save(entity);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FollowUpTask> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(JpaFollowUpTaskRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpTask> findByPlanId(UUID planId) {
        if (planId == null) {
            return List.of();
        }
        return repository.findByPlanIdOrderByDueDateAsc(planId).stream()
                .map(JpaFollowUpTaskRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpTask> findByPlanIdIn(Collection<UUID> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return List.of();
        }
        return repository.findByPlanIdInOrderByDueDateAsc(planIds).stream()
                .map(JpaFollowUpTaskRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpTask> findPendingRemindableInWindow(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            return List.of();
        }
        return repository.findPendingRemindableInWindow(from, to).stream()
                .map(JpaFollowUpTaskRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpTask> findPendingDueBefore(OffsetDateTime now) {
        if (now == null) {
            return List.of();
        }
        return repository.findPendingDueBefore(now).stream()
                .map(JpaFollowUpTaskRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpTask> findOpenDueBefore(Collection<UUID> planIds, OffsetDateTime now) {
        if (planIds == null || planIds.isEmpty() || now == null) {
            return List.of();
        }
        return repository.findOpenDueBefore(planIds, now).stream()
                .map(JpaFollowUpTaskRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingByPlanIdIn(Collection<UUID> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return 0;
        }
        return repository.countPendingByPlanIdIn(planIds);
    }

    private static FollowUpTask toDomain(FollowUpTaskEntity e) {
        return FollowUpTask.of(
                e.getId(),
                e.getPlanId(),
                e.getDescription(),
                e.getDueDate(),
                e.getStatus(),
                e.getCompletedAt(),
                e.isReminderSent(),
                e.getCreatedAt());
    }
}
