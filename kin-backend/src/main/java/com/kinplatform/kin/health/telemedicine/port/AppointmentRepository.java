package com.kinplatform.kin.health.telemedicine.port;

import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de citas de telemedicina (ADR-032).
 *
 * <p>La infraestructura lo implementa con JPA (tabla {@code appointments}).</p>
 */
public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByPhysicianId(UUID physicianId);

    List<Appointment> findByStatus(AppointmentStatus status);
}
