package com.kinplatform.kin.health.scheduling.adapter;

import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import com.kinplatform.kin.health.scheduling.port.PhysicianAvailabilityRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link PhysicianAvailabilityRepository} (ADR-034).
 */
@Component
public class JpaPhysicianAvailabilityRepository implements PhysicianAvailabilityRepository {

    private final PhysicianAvailabilityJpaRepository repository;

    public JpaPhysicianAvailabilityRepository(PhysicianAvailabilityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PhysicianAvailability save(PhysicianAvailability availability) {
        if (availability == null) {
            throw new IllegalArgumentException("availability no puede ser null");
        }
        PhysicianAvailabilityEntity entity = repository
                .findById(availability.id())
                .orElseGet(PhysicianAvailabilityEntity::new);
        entity.setId(availability.id());
        entity.setPhysicianId(availability.physicianId());
        entity.setDayOfWeek(availability.dayOfWeek());
        entity.setStartTime(availability.startTime());
        entity.setEndTime(availability.endTime());
        entity.setSlotDurationMinutes(availability.slotDurationMinutes());
        entity.setActive(availability.active());
        entity.setCreatedAt(availability.createdAt());
        repository.save(entity);
        return availability;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PhysicianAvailability> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id).map(JpaPhysicianAvailabilityRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PhysicianAvailability> findByPhysicianIdAndDayOfWeek(UUID physicianId, DayOfWeek dayOfWeek) {
        if (physicianId == null || dayOfWeek == null) {
            return Optional.empty();
        }
        return repository.findByPhysicianIdAndDayOfWeek(physicianId, dayOfWeek)
                .map(JpaPhysicianAvailabilityRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhysicianAvailability> findActiveByPhysicianId(UUID physicianId) {
        if (physicianId == null) {
            return List.of();
        }
        return repository.findByPhysicianIdAndActiveTrue(physicianId).stream()
                .map(JpaPhysicianAvailabilityRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private static PhysicianAvailability toDomain(PhysicianAvailabilityEntity e) {
        return PhysicianAvailability.of(
                e.getId(),
                e.getPhysicianId(),
                e.getDayOfWeek(),
                e.getStartTime(),
                e.getEndTime(),
                e.getSlotDurationMinutes(),
                e.isActive(),
                e.getCreatedAt());
    }
}
