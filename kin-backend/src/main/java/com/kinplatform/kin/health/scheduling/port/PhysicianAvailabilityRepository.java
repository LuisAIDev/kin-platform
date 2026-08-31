package com.kinplatform.kin.health.scheduling.port;

import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de la disponibilidad del médico (ADR-034).
 */
public interface PhysicianAvailabilityRepository {

    PhysicianAvailability save(PhysicianAvailability availability);

    Optional<PhysicianAvailability> findById(UUID id);

    Optional<PhysicianAvailability> findByPhysicianIdAndDayOfWeek(UUID physicianId, DayOfWeek dayOfWeek);

    /** Horarios ACTIVOS del médico (para calcular slots). */
    List<PhysicianAvailability> findActiveByPhysicianId(UUID physicianId);

    void delete(UUID id);
}
