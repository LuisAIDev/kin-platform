package com.kinplatform.kin.health.dashboard.adapter;

import com.kinplatform.kin.health.dashboard.domain.Reminder.ReminderType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de recordatorios del paciente (ADR-030).
 */
public interface ReminderJpaRepository extends JpaRepository<ReminderEntity, UUID> {

    List<ReminderEntity> findByUserIdAndActiveTrueOrderByScheduledAtAsc(UUID userId);

    List<ReminderEntity> findByUserIdAndActiveTrueAndType(UUID userId, ReminderType type);
}
