package com.kinplatform.kin.health.scheduling.api;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.scheduling.config.SchedulingProperties;
import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import com.kinplatform.kin.health.scheduling.event.AppointmentCanceledEvent;
import com.kinplatform.kin.health.scheduling.event.AppointmentConfirmedEvent;
import com.kinplatform.kin.health.scheduling.event.AppointmentRequestedEvent;
import com.kinplatform.kin.health.scheduling.event.AppointmentRescheduledEvent;
import com.kinplatform.kin.health.scheduling.event.AvailabilityUpdatedEvent;
import com.kinplatform.kin.health.scheduling.port.PhysicianAvailabilityRepository;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación de la agenda y disponibilidad de médicos (ADR-034).
 *
 * <p>Gestión de disponibilidad semanal recurrente, cálculo de slots disponibles
 * con validación de traslapes y ciclo de vida de citas (solicitar, confirmar,
 * cancelar, reprogramar, completar). Todo acceso a un paciente exige relación
 * {@code ACTIVE} (Área 5). Los eventos se publican de forma transaccional vía
 * outbox (fallback al bus en memoria).</p>
 */
@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final PhysicianAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final RelationshipAccessValidator accessValidator;
    private final UserRepository userRepository;
    private final SchedulingProperties properties;
    private final DomainEventBus eventBus;
    private final OutboxEventPublisher outboxEventPublisher;
    private final AuditService auditService;

    public SchedulingService(
            PhysicianAvailabilityRepository availabilityRepository,
            AppointmentRepository appointmentRepository,
            RelationshipAccessValidator accessValidator,
            UserRepository userRepository,
            SchedulingProperties properties,
            AuditService auditService) {
        this(availabilityRepository, appointmentRepository, accessValidator, userRepository, properties, auditService,
                null, null);
    }

    @Autowired
    public SchedulingService(
            PhysicianAvailabilityRepository availabilityRepository,
            AppointmentRepository appointmentRepository,
            RelationshipAccessValidator accessValidator,
            UserRepository userRepository,
            SchedulingProperties properties,
            AuditService auditService,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher) {
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.accessValidator = accessValidator;
        this.userRepository = userRepository;
        this.properties = properties;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    // ---------- Disponibilidad ----------

    @Transactional
    public PhysicianAvailability setAvailability(
            UUID physicianId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            boolean active) {
        requireEnabled();
        requirePhysicianRole(physicianId);

        PhysicianAvailability existing =
                availabilityRepository.findByPhysicianIdAndDayOfWeek(physicianId, dayOfWeek).orElse(null);
        UUID id = existing == null ? UUID.randomUUID() : existing.id();
        OffsetDateTime createdAt = existing == null ? OffsetDateTime.now() : existing.createdAt();
        PhysicianAvailability availability = PhysicianAvailability.of(
                id, physicianId, dayOfWeek, startTime, endTime, slotDurationMinutes, active, createdAt);
        availabilityRepository.save(availability);
        publish(new AvailabilityUpdatedEvent(id, physicianId));
        log.info("SchedulingService: disponibilidad actualizada para médico {} (día {})", physicianId, dayOfWeek);
        return availability;
    }

    @Transactional(readOnly = true)
    public List<PhysicianAvailability> getAvailability(UUID physicianId) {
        requireEnabled();
        return availabilityRepository.findActiveByPhysicianId(physicianId);
    }

    @Transactional
    public void removeAvailability(UUID physicianId, UUID availabilityId) {
        requireEnabled();
        PhysicianAvailability availability = availabilityRepository
                .findById(availabilityId)
                .orElseThrow(() -> new AppointmentAccessDeniedException("Disponibilidad no encontrada"));
        if (!availability.physicianId().equals(physicianId)) {
            throw new AppointmentAccessDeniedException("Solo el médico puede eliminar su disponibilidad");
        }
        availabilityRepository.delete(availabilityId);
    }

    // ---------- Slots ----------

    @Transactional(readOnly = true)
    public List<OffsetDateTime> getAvailableSlots(UUID physicianId, LocalDate date) {
        requireEnabled();
        if (date == null) {
            return List.of();
        }
        PhysicianAvailability availability = availabilityRepository
                .findByPhysicianIdAndDayOfWeek(physicianId, date.getDayOfWeek())
                .filter(PhysicianAvailability::isActive)
                .orElse(null);
        if (availability == null) {
            return List.of();
        }
        ZoneId zone = ZoneId.systemDefault();
        List<OffsetDateTime> slots = new ArrayList<>();
        LocalTime cursor = availability.startTime();
        while (cursor.isBefore(availability.endTime())) {
            slots.add(date.atTime(cursor).atZone(zone).toOffsetDateTime());
            cursor = cursor.plusMinutes(availability.slotDurationMinutes());
        }
        // Filtra los slots ocupados por citas abiertas (PENDIENTE/CONFIRMADA) ese día.
        OffsetDateTime dayStart = date.atStartOfDay().atZone(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay().atZone(zone).toOffsetDateTime();
        List<Appointment> occupied = appointmentRepository
                .findByPhysicianIdAndScheduledAtBetween(physicianId, dayStart, dayEnd)
                .stream()
                .filter(Appointment::isOpen)
                .toList();
        return slots.stream()
                .filter(slot -> !isOccupied(slot, availability.slotDurationMinutes(), occupied))
                .toList();
    }

    /** Slots de un médico para un paciente concreto (valida relación ACTIVE). */
    @Transactional(readOnly = true)
    public List<OffsetDateTime> getAvailableSlotsForPatient(UUID patientId, UUID physicianId, LocalDate date) {
        accessValidator.requireActiveRelationship(physicianId, patientId);
        return getAvailableSlots(physicianId, date);
    }

    // ---------- Citas ----------

    @Transactional
    public Appointment requestAppointment(
            UUID patientId, UUID physicianId, OffsetDateTime scheduledAt, int durationMinutes, String reason) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);
        validateSlot(physicianId, scheduledAt, durationMinutes);

        Appointment appointment = Appointment.of(
                UUID.randomUUID(),
                patientId,
                physicianId,
                scheduledAt,
                durationMinutes,
                reason == null ? "" : reason,
                AppointmentStatus.PENDIENTE,
                OffsetDateTime.now(),
                null,
                "",
                null,
                false);
        Appointment saved = appointmentRepository.save(appointment);
        publish(new AppointmentRequestedEvent(saved.id(), patientId, physicianId));
        auditService.logAccess(patientId, AuditAction.REQUEST_APPOINTMENT, AuditResourceType.CITA, saved.id(),
                patientId, java.util.Map.of());
        log.info("SchedulingService: cita solicitada paciente {} → médico {}", patientId, physicianId);
        return saved;
    }

    @Transactional
    public Appointment confirmAppointment(UUID appointmentId, UUID physicianId) {
        requireEnabled();
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.physicianId().equals(physicianId)) {
            throw new AppointmentAccessDeniedException("Solo el médico de la cita puede confirmarla");
        }
        accessValidator.requireActiveRelationship(physicianId, appointment.patientId());
        Appointment saved = appointmentRepository.save(appointment.confirmed());
        publish(new AppointmentConfirmedEvent(saved.id(), saved.patientId(), physicianId));
        auditService.logAccess(physicianId, AuditAction.CONFIRM_APPOINTMENT, AuditResourceType.CITA, saved.id(),
                saved.patientId(), java.util.Map.of());
        log.info("SchedulingService: cita {} confirmada por médico {}", appointmentId, physicianId);
        return saved;
    }

    @Transactional
    public Appointment cancelAppointment(UUID appointmentId, UUID userId, String reason) {
        requireEnabled();
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.involves(userId)) {
            throw new AppointmentAccessDeniedException("El usuario no participa en esta cita");
        }
        if (userId.equals(appointment.physicianId())) {
            accessValidator.requireActiveRelationship(appointment.physicianId(), appointment.patientId());
        }
        Appointment saved = appointmentRepository.save(appointment.canceled(reason));
        publish(new AppointmentCanceledEvent(saved.id(), saved.patientId(), saved.physicianId(), reason));
        auditService.logAccess(userId, AuditAction.CANCEL_APPOINTMENT, AuditResourceType.CITA, saved.id(),
                saved.patientId(), java.util.Map.of("reason", reason == null ? "" : reason));
        log.info("SchedulingService: cita {} cancelada por {}", appointmentId, userId);
        return saved;
    }

    @Transactional
    public Appointment rescheduleAppointment(UUID appointmentId, UUID userId, OffsetDateTime newScheduledAt, String reason) {
        requireEnabled();
        Appointment old = requireAppointment(appointmentId);
        if (!old.involves(userId)) {
            throw new AppointmentAccessDeniedException("El usuario no participa en esta cita");
        }
        if (userId.equals(old.physicianId())) {
            accessValidator.requireActiveRelationship(old.physicianId(), old.patientId());
        }
        validateSlot(old.physicianId(), newScheduledAt, old.durationMinutes());

        AppointmentStatus newStatus = old.status() == AppointmentStatus.CONFIRMADA
                ? AppointmentStatus.CONFIRMADA
                : AppointmentStatus.PENDIENTE;
        Appointment newAppointment = Appointment.of(
                UUID.randomUUID(),
                old.patientId(),
                old.physicianId(),
                newScheduledAt,
                old.durationMinutes(),
                old.reason(),
                newStatus,
                OffsetDateTime.now(),
                old.id(),
                "",
                null,
                false);
        appointmentRepository.save(old.rescheduled());
        Appointment saved = appointmentRepository.save(newAppointment);
        publish(new AppointmentRescheduledEvent(old.id(), saved.id(), old.patientId(), old.physicianId()));
        auditService.logAccess(userId, AuditAction.RESCHEDULE_APPOINTMENT, AuditResourceType.CITA, saved.id(),
                old.patientId(), java.util.Map.of("from", old.id(), "newScheduledAt", newScheduledAt.toString()));
        log.info("SchedulingService: cita {} reprogramada a {} por {}", appointmentId, newScheduledAt, userId);
        return saved;
    }

    @Transactional
    public Appointment completeAppointment(UUID appointmentId, UUID physicianId) {
        requireEnabled();
        Appointment appointment = requireAppointment(appointmentId);
        if (!appointment.physicianId().equals(physicianId)) {
            throw new AppointmentAccessDeniedException("Solo el médico de la cita puede completarla");
        }
        accessValidator.requireActiveRelationship(physicianId, appointment.patientId());
        Appointment completed = appointmentRepository.save(appointment.completed());
        auditService.logAccess(physicianId, AuditAction.COMPLETE_APPOINTMENT, AuditResourceType.CITA, completed.id(),
                completed.patientId(), java.util.Map.of());
        return completed;
    }

    // ---------- Consultas ----------

    @Transactional(readOnly = true)
    public List<Appointment> upcomingAppointments(UUID userId, boolean asPhysician) {
        requireEnabled();
        OffsetDateTime now = OffsetDateTime.now();
        return asPhysician
                ? appointmentRepository.findUpcomingByPhysicianId(userId, now)
                : appointmentRepository.findUpcomingByPatientId(userId, now);
    }

    @Transactional(readOnly = true)
    public List<Appointment> appointmentHistory(UUID userId, boolean asPhysician) {
        requireEnabled();
        List<Appointment> all = asPhysician
                ? appointmentRepository.findByPhysicianId(userId)
                : appointmentRepository.findByPatientId(userId);
        return all.stream()
                .sorted(java.util.Comparator.comparing(Appointment::scheduledAt).reversed())
                .toList();
    }

    // ---------- Internos ----------

    private void validateSlot(UUID physicianId, OffsetDateTime scheduledAt, int durationMinutes) {
        if (scheduledAt == null || scheduledAt.isBefore(OffsetDateTime.now())) {
            throw new SlotNotAvailableException(scheduledAt == null ? null : scheduledAt.toLocalDateTime());
        }
        PhysicianAvailability availability = availabilityRepository
                .findByPhysicianIdAndDayOfWeek(physicianId, scheduledAt.getDayOfWeek())
                .filter(PhysicianAvailability::isActive)
                .orElseThrow(() -> new SlotNotAvailableException(scheduledAt.toLocalDateTime()));

        LocalTime start = scheduledAt.toLocalTime();
        LocalTime end = start.plusMinutes(durationMinutes);
        if (start.isBefore(availability.startTime()) || end.isAfter(availability.endTime())
                || end.isBefore(availability.startTime())) {
            throw new SlotNotAvailableException(scheduledAt.toLocalDateTime());
        }

        OffsetDateTime from = scheduledAt.minusHours(4);
        OffsetDateTime to = scheduledAt.plusHours(4);
        List<Appointment> around = appointmentRepository
                .findByPhysicianIdAndScheduledAtBetween(physicianId, from, to)
                .stream()
                .filter(Appointment::isOpen)
                .toList();
        if (isOccupied(scheduledAt, durationMinutes, around)) {
            throw new SlotNotAvailableException(scheduledAt.toLocalDateTime());
        }
    }

    private boolean isOccupied(OffsetDateTime start, int durationMinutes, List<Appointment> open) {
        OffsetDateTime end = start.plusMinutes(durationMinutes);
        return open.stream().anyMatch(a -> a.scheduledAt().isBefore(end) && a.endAt().isAfter(start));
    }

    private void requirePhysicianRole(UUID physicianId) {
        userRepository
                .findById(physicianId)
                .filter(u -> u.getRole() == UserRole.PHYSICIAN || u.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new AppointmentAccessDeniedException("Se requiere rol de médico"));
    }

    private Appointment requireAppointment(UUID appointmentId) {
        return appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new SchedulingDisabledException();
        }
    }

    private void publish(DomainEvent event) {
        if (outboxEventPublisher != null) {
            outboxEventPublisher.publish(event);
        } else if (eventBus != null) {
            eventBus.publish(event);
        }
    }
}
