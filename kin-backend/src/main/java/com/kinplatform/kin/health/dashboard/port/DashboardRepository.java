package com.kinplatform.kin.health.dashboard.port;

import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del dashboard de salud (ADR-030).
 *
 * <p>Guarda/carga el perfil del paciente (factores de riesgo y condiciones
 * crónicas) y los recordatorios. El dominio nunca persiste directamente; la
 * infraestructura lo implementa con JPA (tablas {@code patient_profiles} y
 * {@code reminders}).</p>
 */
public interface DashboardRepository {

    Optional<PatientProfile> findProfileByUserId(UUID userId);

    PatientProfile saveProfile(PatientProfile profile);

    Reminder saveReminder(Reminder reminder);

    List<Reminder> findActiveRemindersByUserId(UUID userId);
}
