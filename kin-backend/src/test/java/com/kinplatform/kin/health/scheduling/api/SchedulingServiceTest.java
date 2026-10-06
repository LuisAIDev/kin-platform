package com.kinplatform.kin.health.scheduling.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.physician.access.RelationshipNotActiveException;
import com.kinplatform.kin.health.scheduling.InMemorySchedulingRepositories;
import com.kinplatform.kin.health.scheduling.config.SchedulingProperties;
import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import com.kinplatform.kin.health.scheduling.event.AppointmentCanceledEvent;
import com.kinplatform.kin.health.scheduling.event.AppointmentConfirmedEvent;
import com.kinplatform.kin.health.scheduling.event.AppointmentRequestedEvent;
import com.kinplatform.kin.health.scheduling.event.AppointmentRescheduledEvent;
import com.kinplatform.kin.health.scheduling.event.AvailabilityUpdatedEvent;
import com.kinplatform.kin.health.telemedicine.InMemoryTelemedicineRepositories;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests del servicio de agenda y disponibilidad (ADR-034): disponibilidad,
 * cálculo de slots, traslapes, confirmación, cancelación y reprogramación.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchedulingServiceTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    private InMemoryDomainEventBus bus;
    private SchedulingProperties properties;
    private InMemorySchedulingRepositories scheduling;
    private InMemoryTelemedicineRepositories telemedicine;
    private InMemoryPhysicianRepositories physicians;

    @BeforeEach
    void setUp() {
        bus = new InMemoryDomainEventBus();
        properties = new SchedulingProperties();
        scheduling = new InMemorySchedulingRepositories();
        telemedicine = new InMemoryTelemedicineRepositories();
        physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        when(userRepository.findById(PHYSICIAN))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .email("medico@kin.com")
                        .role(UserRole.PHYSICIAN)
                        .build()));
    }

    private SchedulingService service() {
        var auditProps = new com.kinplatform.common.audit.config.AuditProperties();
        auditProps.setEnabled(false);
        return new SchedulingService(
                scheduling.availabilityRepository(),
                telemedicine.appointmentRepository(),
                new RelationshipAccessValidator(physicians.patientRepository()),
                userRepository,
                properties,
                new com.kinplatform.common.audit.api.AuditService(null, null, null, auditProps),
                bus,
                null);
    }

    private void setDailyAvailability() {
        service().setAvailability(PHYSICIAN, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 30, true);
    }

    private static final LocalDate TEST_MONDAY = LocalDate.of(2030, 1, 7); // lunes futuro determinista

    private OffsetDateTime slot(int minute) {
        return TEST_MONDAY.atTime(9, minute).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
    }

    // ---------- Disponibilidad ----------

    @Test
    void setAvailability_deberiaCrearHorario() {
        var availability = service().setAvailability(
                PHYSICIAN, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), 45, true);

        assertEquals(PHYSICIAN, availability.physicianId());
        assertEquals(1, service().getAvailability(PHYSICIAN).size());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof AvailabilityUpdatedEvent));
    }

    @Test
    void setAvailability_diaExistente_deberiaActualizar() {
        service().setAvailability(PHYSICIAN, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), 45, true);
        service().setAvailability(PHYSICIAN, DayOfWeek.WEDNESDAY, LocalTime.of(15, 0), LocalTime.of(17, 0), 30, true);

        var list = service().getAvailability(PHYSICIAN);
        assertEquals(1, list.size());
        assertEquals(LocalTime.of(15, 0), list.get(0).startTime());
    }

    @Test
    void setAvailability_conUsuarioNoMedico_deberiaLanzar() {
        when(userRepository.findById(PHYSICIAN))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .email("x@kin.com")
                        .role(UserRole.PATIENT)
                        .build()));

        assertThrows(AppointmentAccessDeniedException.class,
                () -> service().setAvailability(PHYSICIAN, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, true));
    }

    // ---------- Slots ----------

    @Test
    void getAvailableSlots_deberiaGenerarSlotsSegunDuracion() {
        setDailyAvailability();

        var slots = service().getAvailableSlots(PHYSICIAN, nextMonday());

        assertEquals(4, slots.size()); // 09:00-11:00 con 30 min → 4 slots
        assertTrue(slots.get(0).toLocalTime().equals(LocalTime.of(9, 0)));
    }

    @Test
    void getAvailableSlots_sinDisponibilidad_deberiaDevolverVacio() {
        var slots = service().getAvailableSlots(PHYSICIAN, nextMonday());

        assertTrue(slots.isEmpty());
    }

    @Test
    void getAvailableSlots_deberiaExcluirSlotOcupado() {
        setDailyAvailability();
        OffsetDateTime taken = slot(0);
        telemedicine.appointmentRepository().save(com.kinplatform.kin.health.telemedicine.domain.Appointment.of(
                UUID.randomUUID(), PATIENT, PHYSICIAN, taken, "motivo",
                AppointmentStatus.CONFIRMADA, OffsetDateTime.now()));

        var slots = service().getAvailableSlots(PHYSICIAN, nextMonday());

        assertEquals(3, slots.size());
        assertTrue(slots.stream().noneMatch(s -> s.equals(taken)));
    }

    // ---------- Citas ----------

    @Test
    void requestAppointment_slotDisponible_deberiaCrearPendiente() {
        setDailyAvailability();
        var appointment = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        assertEquals(AppointmentStatus.PENDIENTE, appointment.status());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof AppointmentRequestedEvent));
    }

    @Test
    void requestAppointment_sinRelacionActiva_deberiaLanzar() {
        var noRel = new InMemoryPhysicianRepositories();
        var auditProps = new com.kinplatform.common.audit.config.AuditProperties();
        auditProps.setEnabled(false);
        var service = new SchedulingService(
                scheduling.availabilityRepository(),
                telemedicine.appointmentRepository(),
                new RelationshipAccessValidator(noRel.patientRepository()),
                userRepository,
                properties,
                new com.kinplatform.common.audit.api.AuditService(null, null, null, auditProps),
                bus,
                null);
        setDailyAvailability();

        assertThrows(RelationshipNotActiveException.class,
                () -> service.requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "x"));
    }

    @Test
    void requestAppointment_slotFueraDeDisponibilidad_deberiaLanzar() {
        setDailyAvailability();

        assertThrows(SlotNotAvailableException.class,
                () -> service().requestAppointment(PATIENT, PHYSICIAN, nextMonday().atTime(12, 0).atZone(
                        java.time.ZoneId.systemDefault()).toOffsetDateTime(), 30, "x"));
    }

    @Test
    void requestAppointment_slotOcupado_deberiaLanzar() {
        setDailyAvailability();
        service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "primera");

        assertThrows(SlotNotAvailableException.class,
                () -> service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "segunda"));
    }

    @Test
    void confirmAppointment_medicoDueño_deberiaConfirmar() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        var confirmed = service().confirmAppointment(appt.id(), PHYSICIAN);

        assertEquals(AppointmentStatus.CONFIRMADA, confirmed.status());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof AppointmentConfirmedEvent));
    }

    @Test
    void confirmAppointment_otroMedico_deberiaLanzar() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        assertThrows(AppointmentAccessDeniedException.class,
                () -> service().confirmAppointment(appt.id(), UUID.randomUUID()));
    }

    @Test
    void cancelAppointment_paciente_deberiaCancelar() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        var canceled = service().cancelAppointment(appt.id(), PATIENT, "ya no puedo");

        assertEquals(AppointmentStatus.CANCELADA, canceled.status());
        assertEquals("ya no puedo", canceled.cancellationReason());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof AppointmentCanceledEvent));
    }

    @Test
    void cancelAppointment_usuarioAjeno_deberiaLanzar() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        assertThrows(AppointmentAccessDeniedException.class,
                () -> service().cancelAppointment(appt.id(), UUID.randomUUID(), "x"));
    }

    @Test
    void rescheduleAppointment_deberiaCrearNuevaYMarcarOriginal() {
        setDailyAvailability();
        var original = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");
        OffsetDateTime newSlot = slot(30);

        var newAppt = service().rescheduleAppointment(original.id(), PATIENT, newSlot, "reprogramada");

        assertEquals(AppointmentStatus.PENDIENTE, newAppt.status());
        assertEquals(original.id(), newAppt.rescheduledFrom());
        var oldNow = telemedicine.appointmentRepository().findById(original.id()).orElseThrow();
        assertEquals(AppointmentStatus.REPROGRAMADA, oldNow.status());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof AppointmentRescheduledEvent));
    }

    @Test
    void rescheduleAppointment_slotOcupado_deberiaLanzar() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");
        service().requestAppointment(PATIENT, PHYSICIAN, slot(30), 30, "otra");

        assertThrows(SlotNotAvailableException.class,
                () -> service().rescheduleAppointment(appt.id(), PATIENT, slot(30), "x"));
    }

    @Test
    void completeAppointment_medico_deberiaCompletar() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        var completed = service().completeAppointment(appt.id(), PHYSICIAN);

        assertEquals(AppointmentStatus.COMPLETADA, completed.status());
    }

    @Test
    void upcomingAppointments_paciente_deberiaDevolverAbiertas() {
        setDailyAvailability();
        var appt = service().requestAppointment(PATIENT, PHYSICIAN, slot(0), 30, "motivo");

        var upcoming = service().upcomingAppointments(PATIENT, false);

        assertEquals(1, upcoming.size());
        assertTrue(upcoming.stream().anyMatch(a -> a.id().equals(appt.id())));
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        properties.setEnabled(false);

        assertThrows(SchedulingDisabledException.class,
                () -> service().getAvailability(PHYSICIAN));
        assertThrows(SchedulingDisabledException.class,
                () -> service().getAvailableSlots(PHYSICIAN, nextMonday()));
    }

    private static LocalDate nextMonday() {
        return TEST_MONDAY;
    }
}

