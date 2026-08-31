package com.kinplatform.kin.health.physician.event;

import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Listener que notifica al paciente por correo cuando recibe una invitación de
 * un médico (ADR-031, ciclo de vida V30).
 *
 * <p>Se suscribe al {@link DomainEventBus} en {@code @PostConstruct} (mismo
 * patrón que {@code ClinicalAlertEventListener}). La entrega es <strong>asíncrona
 * respecto a la API</strong>: el evento se publica de forma transaccional al
 * outbox por {@code RelationshipService} y el {@code OutboxRelay} lo entrega al
 * bus en su hilo de polling (~2 s), de modo que el envío del correo nunca
 * bloquea la respuesta de {@code POST /health/physician/patients/invite}.</p>
 *
 * <p>Resiliencia: cualquier fallo de SMTP se registra con {@code log.error} y
 * no afecta la operación principal (la invitación ya quedó persistida en
 * {@code PENDING}). Si el paciente no tiene email, el módulo está deshabilitado
 * o el correo de invitación está desactivado por flag, no se envía.</p>
 */
@Component
public class PatientInvitedEventListener {

    private static final Logger log = LoggerFactory.getLogger(PatientInvitedEventListener.class);

    private static final String INVITATIONS_PATH = "/dashboard/patient/invitations";

    private final DomainEventBus eventBus;
    private final EmailSender emailSender;
    private final UserRepository userRepository;
    private final PhysicianProperties properties;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public PatientInvitedEventListener(
            DomainEventBus eventBus,
            EmailSender emailSender,
            UserRepository userRepository,
            PhysicianProperties properties) {
        this.eventBus = eventBus;
        this.emailSender = emailSender;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @PostConstruct
    public void register() {
        eventBus.subscribe(PatientInvitedEvent.class, this::onPatientInvited);
        log.info("PatientInvitedEventListener registrado en el bus de eventos");
    }

    void onPatientInvited(PatientInvitedEvent event) {
        if (event == null || event.patientId() == null) {
            return;
        }
        if (!properties.isEnabled() || !properties.isInvitationEmailEnabled()) {
            log.debug("PatientInvitedEventListener: correo de invitación deshabilitado; se omite");
            return;
        }
        User patient = userRepository.findById(event.patientId()).orElse(null);
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            log.warn("PatientInvitedEventListener: paciente sin email válido para invitación (id={})", event.patientId());
            return;
        }
        try {
            String patientName = (patient.getFullName() == null || patient.getFullName().isBlank())
                    ? patient.getEmail()
                    : patient.getFullName();
            String physicianName = (event.physicianName() == null || event.physicianName().isBlank())
                    ? "Médico"
                    : event.physicianName();
            String specialty = specialtyOf(event.physicianId());
            String link = baseUrl() + INVITATIONS_PATH;

            emailSender.sendInvitationEmail(
                    patient.getEmail(), patientName, physicianName, specialty, event.message(), link);
            log.info("PatientInvitedEventListener: correo de invitación enviado a paciente {}", event.patientId());
        } catch (Exception e) {
            // El correo no debe romper el flujo de invitación: se registra y continúa.
            log.error(
                    "PatientInvitedEventListener: fallo al enviar correo de invitación al paciente {}: {}",
                    event.patientId(),
                    e.getMessage(),
                    e);
        }
    }

    private String specialtyOf(UUID physicianId) {
        if (physicianId == null) {
            return null;
        }
        return userRepository.findById(physicianId).map(User::getSpecialty).orElse(null);
    }

    private String baseUrl() {
        return frontendBaseUrl == null || frontendBaseUrl.isBlank() ? "http://localhost:3000" : frontendBaseUrl;
    }
}
