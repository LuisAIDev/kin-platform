package com.kinplatform.kin.health.physician.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
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
 * envía el correo al paciente con los datos del médico y el enlace al panel de
 * invitaciones; los fallos de envío nunca rompen el flujo.
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

    @Test
    void invitacion_deberiaEnviarCorreoConDatosDelMedicoYEnlace() {
        publishEvent("Te invito a mi cartera");

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> patientName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> physicianName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> specialty = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);

        verify(emailSender)
                .sendInvitationEmail(
                        to.capture(), patientName.capture(), physicianName.capture(),
                        specialty.capture(), message.capture(), link.capture());

        assertTrue(to.getValue().endsWith(PATIENT_EMAIL));
        assertTrue(patientName.getValue().equals("Paciente Test"));
        assertTrue(physicianName.getValue().equals("Dr. Ana García"));
        assertTrue(specialty.getValue().equals("Cardiología"));
        assertTrue(message.getValue().equals("Te invito a mi cartera"));
        assertTrue(link.getValue().endsWith("/dashboard/patient/invitations"));
    }

    @Test
    void invitacionSinMensaje_deberiaEnviarConMensajeVacio() {
        publishEvent(null);

        verify(emailSender)
                .sendInvitationEmail(eq(PATIENT_EMAIL), anyString(), anyString(), anyString(), eq(""), anyString());
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
                .sendInvitationEmail(eq(PATIENT_EMAIL), eq(PATIENT_EMAIL), anyString(), anyString(), eq(""), anyString());
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
                .sendInvitationEmail(anyString(), anyString(), anyString(), eq(null), eq(""), anyString());
    }

    @Test
    void correoDeshabilitado_deberiaNoEnviar() {
        properties.setInvitationEmailEnabled(false);

        publishEvent(null);

        verify(emailSender, never())
                .sendInvitationEmail(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void moduloDeshabilitado_deberiaNoEnviar() {
        properties.setEnabled(false);

        publishEvent(null);

        verify(emailSender, never())
                .sendInvitationEmail(anyString(), anyString(), anyString(), any(), anyString(), anyString());
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
                .sendInvitationEmail(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void falloDeEnvio_deberiaNoLanzar() {
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP caído"))
                .when(emailSender)
                .sendInvitationEmail(anyString(), anyString(), anyString(), any(), anyString(), anyString());

        // No debe propagar la excepción (el correo no rompe la invitación).
        publishEvent(null);
        assertFalse(bus.publishedEvents().isEmpty());
    }

    @Test
    void register_deberiaSuscribirseAlBus() {
        publishEvent(null);
        // Si no estuviera suscrito, emailSender nunca recibiría la llamada (verificado arriba).
        verify(emailSender).sendInvitationEmail(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }
}
