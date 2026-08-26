package com.kinplatform.kin.health.telemedicine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.telemedicine.InMemoryTelemedicineRepositories;
import com.kinplatform.kin.health.telemedicine.config.TelemedicineProperties;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelemedicineServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID PHYSICIAN = UUID.randomUUID();

    private static TelemedicineProperties properties(boolean enabled) {
        var props = new TelemedicineProperties();
        props.setEnabled(enabled);
        props.setCryptoSecret("test-secret");
        return props;
    }

    private static TelemedicineService service(
            boolean enabled, InMemoryPhysicianRepositories physicians, InMemoryTelemedicineRepositories repos) {
        return new TelemedicineService(
                repos.messageRepository(),
                repos.appointmentRepository(),
                physicians.patientRepository(),
                properties(enabled));
    }

    private static InMemoryPhysicianRepositories assignedPhysicians() {
        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(PhysicianPatientAssignment.of(PHYSICIAN, PATIENT, OffsetDateTime.now()));
        return physicians;
    }

    @Test
    void sendMessage_pacienteAMedicoAsignado_deberiaEnviar() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);

        var message = service.sendMessage(PATIENT, PHYSICIAN, "Hola doctor, tengo fiebre");

        assertEquals(PATIENT, message.senderId());
        assertEquals(PHYSICIAN, message.receiverId());
        assertFalse(message.read());
        assertEquals(
                1,
                repos.messageRepository()
                        .findByConversationId(message.conversationId())
                        .size());
    }

    @Test
    void sendMessage_sinAsignacion_deberiaLanzar() {
        var service = service(true, new InMemoryPhysicianRepositories(), new InMemoryTelemedicineRepositories());

        assertThrows(TelemedicineAssignmentException.class, () -> service.sendMessage(PATIENT, PHYSICIAN, "hola"));
    }

    @Test
    void sendMessage_aSiMismo_deberiaLanzar() {
        var service = service(true, new InMemoryPhysicianRepositories(), new InMemoryTelemedicineRepositories());

        assertThrows(IllegalArgumentException.class, () -> service.sendMessage(PATIENT, PATIENT, "hola"));
    }

    @Test
    void conversation_deberiaEstarAisladaPorConversacion() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);
        service.sendMessage(PATIENT, PHYSICIAN, "m1");
        service.sendMessage(PHYSICIAN, PATIENT, "m2");

        var conversation = service.conversationMessages(PATIENT, PHYSICIAN);

        assertEquals(2, conversation.size());
        assertEquals("m1", conversation.get(0).content());
        assertEquals("m2", conversation.get(1).content());
    }

    @Test
    void conversationId_deberiaSerSimetrico() {
        assertEquals(
                com.kinplatform.kin.health.telemedicine.domain.Message.conversationIdOf(PATIENT, PHYSICIAN),
                com.kinplatform.kin.health.telemedicine.domain.Message.conversationIdOf(PHYSICIAN, PATIENT));
    }

    @Test
    void markConversationRead_deberiaMarcarLosDirigidosAlUsuario() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);
        service.sendMessage(PHYSICIAN, PATIENT, "m1");
        service.sendMessage(PHYSICIAN, PATIENT, "m2");

        assertEquals(2, service.unreadCount(PATIENT));
        service.markConversationRead(PATIENT, PHYSICIAN);

        assertEquals(0, service.unreadCount(PATIENT));
        assertTrue(service.conversationMessages(PATIENT, PHYSICIAN).stream().allMatch(m -> m.read()));
    }

    @Test
    void requestAppointment_pacienteAMedicoAsignado_deberiaCrearPendiente() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);

        var appointment = service.requestAppointment(
                PATIENT, PHYSICIAN, OffsetDateTime.now().plusDays(2), "Control de hipertensión");

        assertEquals(AppointmentStatus.PENDIENTE, appointment.status());
        assertEquals(PATIENT, appointment.patientId());
        assertEquals(1, service.listAppointments(PATIENT, false).size());
    }

    @Test
    void requestAppointment_sinAsignacion_deberiaLanzar() {
        var service = service(true, new InMemoryPhysicianRepositories(), new InMemoryTelemedicineRepositories());

        assertThrows(
                TelemedicineAssignmentException.class,
                () -> service.requestAppointment(
                        PATIENT, PHYSICIAN, OffsetDateTime.now().plusDays(1), "motivo"));
    }

    @Test
    void updateAppointmentStatus_medico_deberiaConfirmar() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);
        var appointment = service.requestAppointment(
                PATIENT, PHYSICIAN, OffsetDateTime.now().plusDays(2), "Control");

        var confirmed = service.updateAppointmentStatus(PHYSICIAN, appointment.id(), AppointmentStatus.CONFIRMADA);

        assertEquals(AppointmentStatus.CONFIRMADA, confirmed.status());
        assertTrue(service.listAppointments(PHYSICIAN, true).stream()
                .anyMatch(a -> a.id().equals(appointment.id()) && a.status() == AppointmentStatus.CONFIRMADA));
    }

    @Test
    void updateAppointmentStatus_otroMedico_deberiaLanzar() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);
        var appointment = service.requestAppointment(
                PATIENT, PHYSICIAN, OffsetDateTime.now().plusDays(2), "Control");

        assertThrows(
                TelemedicineAppointmentNotFoundException.class,
                () -> service.updateAppointmentStatus(
                        UUID.randomUUID(), appointment.id(), AppointmentStatus.CONFIRMADA));
    }

    @Test
    void updateAppointmentStatus_estadoPendiente_deberiaLanzar() {
        var repos = new InMemoryTelemedicineRepositories();
        var service = service(true, assignedPhysicians(), repos);
        var appointment = service.requestAppointment(
                PATIENT, PHYSICIAN, OffsetDateTime.now().plusDays(2), "Control");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAppointmentStatus(PHYSICIAN, appointment.id(), AppointmentStatus.PENDIENTE));
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        var service = service(false, assignedPhysicians(), new InMemoryTelemedicineRepositories());

        assertThrows(TelemedicineDisabledException.class, () -> service.sendMessage(PATIENT, PHYSICIAN, "hola"));
        assertThrows(TelemedicineDisabledException.class, () -> service.listAppointments(PATIENT, false));
        assertThrows(
                TelemedicineDisabledException.class,
                () -> service.requestAppointment(PATIENT, PHYSICIAN, OffsetDateTime.now(), "x"));
    }
}
