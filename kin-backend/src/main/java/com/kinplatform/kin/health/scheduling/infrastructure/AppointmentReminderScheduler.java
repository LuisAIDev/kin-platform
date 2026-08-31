package com.kinplatform.kin.health.scheduling.infrastructure;

import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.dashboard.port.DashboardRepository;
import com.kinplatform.kin.health.scheduling.config.SchedulingProperties;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesos programados de la agenda (ADR-034).
 *
 * <p>Ejecución diaria (08:00): crea recordatorios dentro de la plataforma
 * (tabla {@code reminders}) y envía un correo al paciente para las citas
 * confirmadas en las próximas 48 h (deduplicado con {@code reminderSent}); y
 * recuerda al médico las citas pendientes de confirmar con más de
 * {@code confirmationReminderHours}.</p>
 */
@Component
public class AppointmentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);

    private static final int LOOKAHEAD_HOURS = 48;

    private final AppointmentRepository appointmentRepository;
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final SchedulingProperties properties;

    public AppointmentReminderScheduler(
            AppointmentRepository appointmentRepository,
            DashboardRepository dashboardRepository,
            UserRepository userRepository,
            EmailSender emailSender,
            SchedulingProperties properties) {
        this.appointmentRepository = appointmentRepository;
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processAppointmentReminders() {
        if (!properties.isEnabled()) {
            return;
        }
        remindPatientsOfConfirmedAppointments();
        remindPhysiciansOfPendingConfirmation();
    }

    /** Recordatorio (plataforma + correo) de citas confirmadas en las próximas 48 h. */
    void remindPatientsOfConfirmedAppointments() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plusHours(LOOKAHEAD_HOURS);
        List<Appointment> confirmed = appointmentRepository.findConfirmedByScheduledAtBetween(now, windowEnd);
        for (Appointment appointment : confirmed) {
            if (appointment.reminderSent()) {
                continue;
            }
            User patient = userRepository.findById(appointment.patientId()).orElse(null);
            User physician = userRepository.findById(appointment.physicianId()).orElse(null);
            if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
                continue;
            }
            String physicianName = physician == null ? "tu médico" : physician.getFullName();
            String scheduledText = formatDateTime(appointment.scheduledAt());
            dashboardRepository.saveReminder(Reminder.of(
                    UUID.randomUUID(),
                    appointment.patientId(),
                    Reminder.ReminderType.GENERAL,
                    "Cita confirmada con " + physicianName + " el " + scheduledText,
                    appointment.scheduledAt().minusMinutes(30),
                    true,
                    now));
            try {
                emailSender.sendAppointmentReminderEmail(
                        patient.getEmail(),
                        patientName(patient),
                        physicianName,
                        scheduledText);
            } catch (Exception e) {
                log.error("AppointmentReminderScheduler: fallo al enviar recordatorio de la cita {}: {}",
                        appointment.id(), e.getMessage());
            }
            appointmentRepository.save(appointment.withReminderSent());
            log.info("AppointmentReminderScheduler: recordatorio de cita {} enviado al paciente {}",
                    appointment.id(), appointment.patientId());
        }
    }

    /** Recuerda al médico las citas pendientes de confirmar con más de X horas. */
    void remindPhysiciansOfPendingConfirmation() {
        OffsetDateTime threshold = OffsetDateTime.now()
                .minusHours(Math.max(1, properties.getConfirmationReminderHours()));
        List<Appointment> pending = appointmentRepository.findPendingByCreatedAtBefore(threshold);
        for (Appointment appointment : pending) {
            if (hasPendingConfirmationReminder(appointment.physicianId(), appointment.id())) {
                continue;
            }
            dashboardRepository.saveReminder(Reminder.of(
                    UUID.randomUUID(),
                    appointment.physicianId(),
                    Reminder.ReminderType.GENERAL,
                    "Cita pendiente de confirmar [" + shortId(appointment.id()) + "] del paciente "
                            + patientName(userRepository.findById(appointment.patientId()).orElse(null)),
                    OffsetDateTime.now().plusHours(2),
                    true,
                    OffsetDateTime.now()));
            log.info("AppointmentReminderScheduler: recordatorio de confirmación de la cita {} al médico {}",
                    appointment.id(), appointment.physicianId());
        }
    }

    private boolean hasPendingConfirmationReminder(UUID physicianId, UUID appointmentId) {
        String marker = "[" + shortId(appointmentId) + "]";
        return dashboardRepository.findActiveRemindersByUserId(physicianId).stream()
                .anyMatch(r -> r.title() != null && r.title().contains(marker));
    }

    private static String patientName(User user) {
        return user == null ? "paciente" : (user.getFullName() == null || user.getFullName().isBlank()
                ? user.getEmail() : user.getFullName());
    }

    private static String formatDateTime(OffsetDateTime when) {
        return when.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
