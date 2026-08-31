package com.kinplatform.kin.health.followup.infrastructure;

import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.dashboard.port.DashboardRepository;
import com.kinplatform.kin.health.followup.config.FollowUpProperties;
import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.domain.FollowUpTask;
import com.kinplatform.kin.health.followup.port.FollowUpPlanRepository;
import com.kinplatform.kin.health.followup.port.FollowUpTaskRepository;
import com.kinplatform.kin.health.followup.port.PatientEvolutionRepository;
import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesos programados del seguimiento de pacientes (ADR-033).
 *
 * <p>Ejecución diaria (08:00): marca las tareas vencidas, genera recordatorios
 * dentro de la plataforma (tabla {@code reminders}) para las tareas que vencen
 * pronto y crea una alerta para el médico cuando un paciente con plan activo no
 * registra evolución en N días. Todo es determinista y eficiente (consultas
 * indexadas); los recordatorios se deduplican con {@code reminder_sent}.</p>
 */
@Component
public class FollowUpReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(FollowUpReminderScheduler.class);

    private final FollowUpPlanRepository planRepository;
    private final FollowUpTaskRepository taskRepository;
    private final PatientEvolutionRepository evolutionRepository;
    private final DashboardRepository dashboardRepository;
    private final ClinicalAlertRepository alertRepository;
    private final FollowUpProperties properties;

    public FollowUpReminderScheduler(
            FollowUpPlanRepository planRepository,
            FollowUpTaskRepository taskRepository,
            PatientEvolutionRepository evolutionRepository,
            DashboardRepository dashboardRepository,
            ClinicalAlertRepository alertRepository,
            FollowUpProperties properties) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.evolutionRepository = evolutionRepository;
        this.dashboardRepository = dashboardRepository;
        this.alertRepository = alertRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processFollowUp() {
        if (!properties.isEnabled()) {
            return;
        }
        markOverdueTasks();
        createTaskReminders();
        createStaleEvolutionAlerts();
    }

    /** Convierte en OVERDUE las tareas PENDING cuya fecha ya venció. */
    void markOverdueTasks() {
        OffsetDateTime now = OffsetDateTime.now();
        List<FollowUpTask> overdue = taskRepository.findPendingDueBefore(now);
        for (FollowUpTask task : overdue) {
            taskRepository.save(task.overdue());
        }
        if (!overdue.isEmpty()) {
            log.info("FollowUpReminderScheduler: {} tareas marcadas como vencidas", overdue.size());
        }
    }

    /** Crea un recordatorio (dentro de la plataforma) para tareas que vencen pronto. */
    void createTaskReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plusDays(Math.max(1, properties.getReminderDaysBefore()));
        List<FollowUpTask> dueSoon = taskRepository.findPendingRemindableInWindow(now, windowEnd);
        for (FollowUpTask task : dueSoon) {
            FollowUpPlan plan = planRepository.findById(task.planId()).orElse(null);
            if (plan == null) {
                continue;
            }
            dashboardRepository.saveReminder(Reminder.of(
                    UUID.randomUUID(),
                    plan.patientId(),
                    Reminder.ReminderType.GENERAL,
                    "Tarea de seguimiento: " + task.description(),
                    task.dueDate(),
                    true,
                    now));
            taskRepository.save(task.withReminderSent());
            log.info("FollowUpReminderScheduler: recordatorio creado para la tarea {} del paciente {}",
                    task.id(), plan.patientId());
        }
    }

    /** Alerta al médico si un paciente con plan ACTIVO no registra evolución en N días. */
    void createStaleEvolutionAlerts() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusDays(Math.max(1, properties.getEvolutionAlertDays()));
        List<UUID> physicianIds = planRepository.findDistinctPhysicianIdsWithActivePlans();
        for (UUID physicianId : physicianIds) {
            for (FollowUpPlan plan : planRepository.findByPhysicianId(physicianId)) {
                if (!plan.isActive()) {
                    continue;
                }
                if (hasPendingStaleAlert(physicianId, plan.patientId())) {
                    continue;
                }
                boolean stale = evolutionRepository
                        .findLatestByPatientId(plan.patientId())
                        .map(ev -> ev.recordedAt() == null || ev.recordedAt().isBefore(threshold))
                        .orElse(true);
                if (stale) {
                    alertRepository.save(ClinicalAlert.of(
                            UUID.randomUUID(),
                            plan.patientId(),
                            physicianId,
                            ClinicalAlert.AlertType.EVOLUTION_STALE,
                            ClinicalAlert.AlertSeverity.MEDIA,
                            "El paciente no registra evolución de su plan de seguimiento en más de "
                                    + properties.getEvolutionAlertDays() + " días",
                            ClinicalAlert.AlertStatus.PENDING,
                            now,
                            null));
                    log.info("FollowUpReminderScheduler: alerta de evolución sin registrar para paciente {} → médico {}",
                            plan.patientId(), physicianId);
                }
            }
        }
    }

    private boolean hasPendingStaleAlert(UUID physicianId, UUID patientId) {
        return alertRepository.findActiveByPhysician(physicianId).stream()
                .anyMatch(a -> a.patientId().equals(patientId)
                        && a.type() == ClinicalAlert.AlertType.EVOLUTION_STALE);
    }
}
