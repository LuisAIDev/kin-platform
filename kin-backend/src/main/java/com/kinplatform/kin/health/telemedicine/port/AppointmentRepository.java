package com.kinplatform.kin.health.telemedicine.port;

import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de citas de telemedicina/agenda (ADR-032 + ADR-034).
 *
 * <p>La infraestructura lo implementa con JPA (tabla {@code appointments}).</p>
 */
public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByPhysicianId(UUID physicianId);

    List<Appointment> findByStatus(AppointmentStatus status);

    /** Citas pendientes de confirmación del médico (contador de notificaciones). */
    long countPendingByPhysician(UUID physicianId);

    /** Próximas citas del paciente (PENDIENTE/CONFIRMADA con fecha >= {@code from}). */
    long countUpcomingByPatient(UUID patientId, OffsetDateTime from);

    /** Citas de un médico en un rango (validación de traslapes y slots ocupados). */
    List<Appointment> findByPhysicianIdAndScheduledAtBetween(
            UUID physicianId, OffsetDateTime from, OffsetDateTime to);

    /** Citas de un paciente en un rango (validación de traslapes). */
    List<Appointment> findByPatientIdAndScheduledAtBetween(
            UUID patientId, OffsetDateTime from, OffsetDateTime to);

    /** Citas confirmadas en un rango (recordatorios automáticos). */
    List<Appointment> findConfirmedByScheduledAtBetween(OffsetDateTime from, OffsetDateTime to);

    /** Citas PENDIENTE creadas antes de {@code before} (recordatorio de confirmación al médico). */
    List<Appointment> findPendingByCreatedAtBefore(OffsetDateTime before);

    /** Próximas citas abiertas (PENDIENTE/CONFIRMADA) de un paciente. */
    List<Appointment> findUpcomingByPatientId(UUID patientId, OffsetDateTime now);

    /** Próximas citas abiertas (PENDIENTE/CONFIRMADA) de un médico. */
    List<Appointment> findUpcomingByPhysicianId(UUID physicianId, OffsetDateTime now);
}