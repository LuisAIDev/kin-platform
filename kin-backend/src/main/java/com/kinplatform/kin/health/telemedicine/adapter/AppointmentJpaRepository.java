package com.kinplatform.kin.health.telemedicine.adapter;

import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de citas de telemedicina (ADR-032).
 */
public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, UUID> {

    List<AppointmentEntity> findByPatientIdOrderByScheduledAtAsc(UUID patientId);

    List<AppointmentEntity> findByPhysicianIdOrderByScheduledAtAsc(UUID physicianId);

    List<AppointmentEntity> findByStatus(AppointmentStatus status);
}
