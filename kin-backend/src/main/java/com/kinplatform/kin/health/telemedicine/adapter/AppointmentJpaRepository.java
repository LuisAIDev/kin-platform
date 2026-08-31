package com.kinplatform.kin.health.telemedicine.adapter;

import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de citas de telemedicina/agenda (ADR-032 + ADR-034).
 */
public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, UUID> {

    List<AppointmentEntity> findByPatientIdOrderByScheduledAtAsc(UUID patientId);

    List<AppointmentEntity> findByPhysicianIdOrderByScheduledAtAsc(UUID physicianId);

    List<AppointmentEntity> findByStatus(AppointmentStatus status);

    long countByPhysicianIdAndStatus(UUID physicianId, AppointmentStatus status);

    long countByPatientIdAndStatusInAndScheduledAtGreaterThanEqual(
            UUID patientId, Collection<AppointmentStatus> statuses, OffsetDateTime from);

    List<AppointmentEntity> findByPhysicianIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            UUID physicianId, OffsetDateTime from, OffsetDateTime to);

    List<AppointmentEntity> findByPatientIdAndScheduledAtBetweenOrderByScheduledAtAsc(
            UUID patientId, OffsetDateTime from, OffsetDateTime to);

    List<AppointmentEntity> findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
            AppointmentStatus status, OffsetDateTime from, OffsetDateTime to);

    List<AppointmentEntity> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            AppointmentStatus status, OffsetDateTime before);

    List<AppointmentEntity> findByPatientIdAndStatusInAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
            UUID patientId, Collection<AppointmentStatus> statuses, OffsetDateTime from);

    List<AppointmentEntity> findByPhysicianIdAndStatusInAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(
            UUID physicianId, Collection<AppointmentStatus> statuses, OffsetDateTime from);
}