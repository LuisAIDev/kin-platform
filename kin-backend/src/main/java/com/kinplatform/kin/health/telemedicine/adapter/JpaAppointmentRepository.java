package com.kinplatform.kin.health.telemedicine.adapter;

import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
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
        entity.setReason(appointment.reason());
        entity.setStatus(appointment.status());
        entity.setCreatedAt(appointment.createdAt());
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

    private Appointment toDomain(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Appointment.of(
                entity.getId(),
                entity.getPatientId(),
                entity.getPhysicianId(),
                entity.getScheduledAt(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
