package com.kinplatform.kin.health.telemedicine.adapter;

import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link AppointmentRepository} (ADR-032).
 */
@Component
public class JpaAppointmentRepository implements AppointmentRepository {

    private final AppointmentJpaRepository repository;

    public JpaAppointmentRepository(AppointmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Appointment save(Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("appointment no puede ser null");
        }
        AppointmentEntity entity = repository.findById(appointment.id()).orElseGet(AppointmentEntity::new);
        entity.setId(appointment.id());
        entity.setPatientId(appointment.patientId());
        entity.setPhysicianId(appointment.physicianId());
        entity.setScheduledAt(appointment.scheduledAt());
        entity.setDurationMinutes(appointment.durationMinutes());
        entity.setReason(appointment.reason());
        entity.setStatus(appointment.status());
        entity.setCreatedAt(appointment.createdAt());
        entity.setRescheduledFrom(appointment.rescheduledFrom());
        entity.setCancellationReason(appointment.cancellationReason());
        entity.setAvailabilitySlotId(appointment.availabilitySlotId());
        entity.setReminderSent(appointment.reminderSent());
        AppointmentEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Appointment> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPatientId(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return repository.findByPatientIdOrderByScheduledAtAsc(patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPhysicianId(UUID physicianId) {
        if (physicianId == null) {
            return List.of();
        }
        return repository.findByPhysicianIdOrderByScheduledAtAsc(physicianId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return repository.findByStatus(status).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingByPhysician(UUID physicianId) {
        if (physicianId == null) {
            return 0;
        }
        return repository.countByPhysicianIdAndStatus(physicianId, AppointmentStatus.PENDIENTE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUpcomingByPatient(UUID patientId, OffsetDateTime from) {
        if (patientId == null || from == null) {
            return 0;
        }
        return repository.countByPatientIdAndStatusInAndScheduledAtGreaterThanEqual(
                patientId,
                java.util.List.of(AppointmentStatus.PENDIENTE, AppointmentStatus.CONFIRMADA),
                from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPhysicianIdAndScheduledAtBetween(UUID physicianId, OffsetDateTime from, OffsetDateTime to) {
        if (physicianId == null || from == null || to == null) {
            return List.of();
        }
        return repository.findByPhysicianIdAndScheduledAtBetweenOrderByScheduledAtAsc(physicianId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPatientIdAndScheduledAtBetween(UUID patientId, OffsetDateTime from, OffsetDateTime to) {
        if (patientId == null || from == null || to == null) {
            return List.of();
        }
        return repository.findByPatientIdAndScheduledAtBetweenOrderByScheduledAtAsc(patientId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findConfirmedByScheduledAtBetween(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            return List.of();
        }
        return repository.findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(AppointmentStatus.CONFIRMADA, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findPendingByCreatedAtBefore(OffsetDateTime before) {
        if (before == null) {
            return List.of();
        }
        return repository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(AppointmentStatus.PENDIENTE, before).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findUpcomingByPatientId(UUID patientId, OffsetDateTime now) {
        if (patientId == null || now == null) {
            return List.of();
        }
        return repository
                .findByPatientIdAndStatusInAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                        patientId, java.util.List.of(AppointmentStatus.PENDIENTE, AppointmentStatus.CONFIRMADA), now)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findUpcomingByPhysicianId(UUID physicianId, OffsetDateTime now) {
        if (physicianId == null || now == null) {
            return List.of();
        }
        return repository
                .findByPhysicianIdAndStatusInAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
                        physicianId, java.util.List.of(AppointmentStatus.PENDIENTE, AppointmentStatus.CONFIRMADA), now)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Appointment toDomain(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Appointment.of(
                entity.getId(),
                entity.getPatientId(),
                entity.getPhysicianId(),
                entity.getScheduledAt(),
                entity.getDurationMinutes(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRescheduledFrom(),
                entity.getCancellationReason(),
                entity.getAvailabilitySlotId(),
                entity.isReminderSent());
    }
}
