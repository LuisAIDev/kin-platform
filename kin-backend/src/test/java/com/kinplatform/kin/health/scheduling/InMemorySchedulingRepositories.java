package com.kinplatform.kin.health.scheduling;

import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import com.kinplatform.kin.health.scheduling.port.PhysicianAvailabilityRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del puerto de disponibilidad para tests (ADR-034).
 */
public class InMemorySchedulingRepositories {

    private final java.util.Map<UUID, PhysicianAvailability> availabilities = new ConcurrentHashMap<>();

    public PhysicianAvailabilityRepository availabilityRepository() {
        return new PhysicianAvailabilityRepository() {
            @Override
            public PhysicianAvailability save(PhysicianAvailability availability) {
                availabilities.put(availability.id(), availability);
                return availability;
            }

            @Override
            public Optional<PhysicianAvailability> findById(UUID id) {
                return Optional.ofNullable(availabilities.get(id));
            }

            @Override
            public Optional<PhysicianAvailability> findByPhysicianIdAndDayOfWeek(UUID physicianId, DayOfWeek dayOfWeek) {
                return availabilities.values().stream()
                        .filter(a -> a.physicianId().equals(physicianId) && a.dayOfWeek() == dayOfWeek)
                        .findFirst();
            }

            @Override
            public List<PhysicianAvailability> findActiveByPhysicianId(UUID physicianId) {
                return availabilities.values().stream()
                        .filter(a -> a.physicianId().equals(physicianId) && a.isActive())
                        .toList();
            }

            @Override
            public void delete(UUID id) {
                availabilities.remove(id);
            }
        };
    }

    public static PhysicianAvailability availability(
            UUID physicianId, DayOfWeek dayOfWeek, java.time.LocalTime start, java.time.LocalTime end, int duration) {
        return PhysicianAvailability.of(
                UUID.randomUUID(), physicianId, dayOfWeek, start, end, duration, true, java.time.OffsetDateTime.now());
    }
}
