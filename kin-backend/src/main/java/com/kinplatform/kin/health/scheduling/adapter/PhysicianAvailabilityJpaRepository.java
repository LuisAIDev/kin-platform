package com.kinplatform.kin.health.scheduling.adapter;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de disponibilidad del médico (ADR-034).
 */
public interface PhysicianAvailabilityJpaRepository extends JpaRepository<PhysicianAvailabilityEntity, UUID> {

    Optional<PhysicianAvailabilityEntity> findByPhysicianIdAndDayOfWeek(UUID physicianId, DayOfWeek dayOfWeek);

    List<PhysicianAvailabilityEntity> findByPhysicianIdAndActiveTrue(UUID physicianId);
}
