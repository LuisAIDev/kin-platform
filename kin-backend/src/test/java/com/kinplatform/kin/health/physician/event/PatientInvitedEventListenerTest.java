package com.kinplatform.kin.health.physician.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.auth.email.EmailSender;
import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests del listener de correo de invitación (ADR-031, ciclo de vida V30):
 * envía el correo al paciente con los datos del médico, el enlace al panel de
 * invitaciones y, si hace falta, la nota de consentimiento de datos de salud.
 * Un fallo de envío se PROPAGA para que el outbox reintente la entrega.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientInvitedEventListenerTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private EmailSender emailSender;

    @Mock
    private UserRepository userRepository;

    private PhysicianProperties properties;
    private InMemoryDomainEventBus bus;
    private PatientInvitedEventListener listener;

    @BeforeEach
    void setUp() {
        properties = new PhysicianProperties();
        bus = new InMemoryDomainEventBus();
        listener = new PatientInvitedEventListener(bus, emailSender, userRepository, properties);
        listener.register();

        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .fullName("Paciente Test")
                        .role(UserRole.PATIENT)
                        .build()));
        when(userRepository.findById(PHYSICIAN))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .email("medico@kin.com")
                        .fullName("Dr. Ana García")
                        .specialty("Cardiología")
                        .role(UserRole.PHYSICIAN)
                        .build()));
    }

    private void publishEvent(String message) {
        bus.publish(new PatientInvitedEvent(PATIENT, PHYSICIAN, "Dr. Ana García", message));
    }

    private void publishResendEvent(String message) {
        bus.publish(new PatientInvitedEvent(PATIENT, PHYSICIAN, "Dr. Ana García", message, true));
    }

    @Test
    void reenvio_deberiaEnviarCorreoDeRecordatorioConLosMismosDatos() {
        publishResendEvent("Te recuerdo la invitación");

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> patientName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> physicianName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> specialty = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> consentRequired = ArgumentCaptor.forClass(Boolean.class);

        verify(emailSender)
                .sendInvitationReminderEmail(
                        to.capture(),
                        patientName.capture(),
                        physicianName.capture(),
                        specialty.capture(),
                        message.capture(),
                        link.capture(),
                        consentRequired.capture());
        verify(emailSender, never())
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());

        assertTrue(to.getValue().endsWith(PATIENT_EMAIL));
        assertTrue(message.getValue().equals("Te recuerdo la invitación"));
        assertTrue(link.getValue().endsWith("/dashboard/patient/invitations"));
        assertFalse(consentRequired.getValue());
    }

    @Test
    void reenvioSinMensaje_deberiaEnviarRecordatorioConMensajeVacio() {
        publishResendEvent(null);

        verify(emailSender)
                .sendInvitationReminderEmail(
                        eq(PATIENT_EMAIL), anyString(), anyString(), anyString(), eq(""), anyString(), anyBoolean());
    }

    @Test
    void invitacionInicial_deberiaUsarCorreoNormalYNoRecordatorio() {
        publishEvent(null);

        verify(emailSender)
                .sendInvitationEmail(
                        eq(PATIENT_EMAIL), anyString(), anyString(), anyString(), eq(""), anyString(), anyBoolean());
        verify(emailSender, never())
                .sendInvitationReminderEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void invitacion_deberiaEnviarCorreoConDatosDelMedicoYEnlace() {
        publishEvent("Te invito a mi cartera");

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> patientName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> physicianName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> specialty = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> consentRequired = ArgumentCaptor.forClass(Boolean.class);

        verify(emailSender)
                .sendInvitationEmail(
                        to.capture(),
                        patientName.capture(),
                        physicianName.capture(),
                        specialty.capture(),
                        message.capture(),
                        link.capture(),
                        consentRequired.capture());

        assertTrue(to.getValue().endsWith(PATIENT_EMAIL));
        assertTrue(patientName.getValue().equals("Paciente Test"));
        assertTrue(physicianName.getValue().equals("Dr. Ana García"));
        assertTrue(specialty.getValue().equals("Cardiología"));
        assertTrue(message.getValue().equals("Te invito a mi cartera"));
        assertTrue(link.getValue().endsWith("/dashboard/patient/invitations"));
        // PATIENT legacy ya es paciente: no debe pedir consentimiento.
        assertFalse(consentRequired.getValue());
    }

    @Test
    void invitacionSinMensaje_deberiaEnviarConMensajeVacio() {
        publishEvent(null);

        verify(emailSender)
                .sendInvitationEmail(
                        eq(PATIENT_EMAIL), anyString(), anyString(), anyString(), eq(""), anyString(), anyBoolean());
    }

    @Test
    void pacienteSinNombre_deberiaUsarEmailComoSaludo() {
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .role(UserRole.PATIENT)
                        .build()));

        publishEvent(null);

        verify(emailSender)
                .sendInvitationEmail(
                        eq(PATIENT_EMAIL),
                        eq(PATIENT_EMAIL),
                        anyString(),
                        anyString(),
                        eq(""),
                        anyString(),
                        anyBoolean());
    }

    @Test
    void medicoSinEspecialidad_deberiaEnviarSinEspecialidad() {
        when(userRepository.findById(PHYSICIAN))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .email("medico@kin.com")
                        .fullName("Dr. Test")
                        .role(UserRole.PHYSICIAN)
                        .build()));

        publishEvent(null);

        verify(emailSender)
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), eq(null), eq(""), anyString(), anyBoolean());
    }

    @Test
    void pacienteSinConsentimiento_deberiaMarcarConsentimientoPendiente() {
        // FREE sin health_data_consent: no tiene capacidad de paciente -> consentRequired=true
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .fullName("Paciente Test")
                        .role(UserRole.FREE)
                        .healthDataConsent(false)
                        .build()));

        publishEvent(null);

        ArgumentCaptor<Boolean> consentRequired = ArgumentCaptor.forClass(Boolean.class);
        verify(emailSender)
                .sendInvitationEmail(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        anyString(),
                        anyString(),
                        consentRequired.capture());
        assertTrue(consentRequired.getValue());
    }

    @Test
    void pacienteConConsentimiento_noDeberiaMarcarConsentimientoPendiente() {
        // FREE con health_data_consent=true: ya tiene capacidad de paciente
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .fullName("Paciente Test")
                        .role(UserRole.FREE)
                        .healthDataConsent(true)
                        .build()));

        publishEvent(null);

        ArgumentCaptor<Boolean> consentRequired = ArgumentCaptor.forClass(Boolean.class);
        verify(emailSender)
                .sendInvitationEmail(
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        anyString(),
                        anyString(),
                        consentRequired.capture());
        assertFalse(consentRequired.getValue());
    }

    @Test
    void correoDeshabilitado_deberiaNoEnviar() {
        properties.setInvitationEmailEnabled(false);

        publishEvent(null);

        verify(emailSender, never())
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void moduloDeshabilitado_deberiaNoEnviar() {
        properties.setEnabled(false);

        publishEvent(null);

        verify(emailSender, never())
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void pacienteSinEmail_deberiaNoEnviar() {
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(null)
                        .fullName("Paciente Test")
                        .role(UserRole.PATIENT)
                        .build()));

        publishEvent(null);

        verify(emailSender, never())
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void falloDeEnvio_deberiaPropagarParaReintentoDelOutbox() {
        doThrow(new IllegalStateException("SMTP caído"))
                .when(emailSender)
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());

        // El fallo se PROPAGA: en producción el OutboxRelay lo reintentará hasta
        // DEAD_LETTER (la invitación PENDING ya quedó persistida en otra transacción).
        assertThrows(IllegalStateException.class, () -> publishEvent(null));
        assertFalse(bus.publishedEvents().isEmpty());
    }

    @Test
    void register_deberiaSuscribirseAlBus() {
        publishEvent(null);
        // Si no estuviera suscrito, emailSender nunca recibiría la llamada (verificado arriba).
        verify(emailSender)
                .sendInvitationEmail(
                        anyString(), anyString(), anyString(), any(), anyString(), anyString(), anyBoolean());
    }
}


