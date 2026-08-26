package com.kinplatform.kin.health.dashboard.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.test.PostgresTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del adaptador JPA del dashboard con PostgreSQL real
 * (Testcontainers). Verifica que Flyway V24 crea las tablas y que el perfil y
 * los recordatorios persisten/consultan correctamente (JSONB).
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaDashboardRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    private JpaDashboardRepository dashboardRepository;

    @Test
    @Transactional
    void perfil_deberiaGuardarYRecuperar() {
        UUID userId = UUID.randomUUID();
        PatientProfile profile = PatientProfile.of(
                userId, List.of("fumador", "diabetes"), List.of("hipertensión"), OffsetDateTime.now());

        dashboardRepository.saveProfile(profile);
        var loaded = dashboardRepository.findProfileByUserId(userId).orElseThrow();

        assertEquals(userId, loaded.userId());
        assertTrue(loaded.riskFactors().contains("fumador"));
        assertTrue(loaded.chronicConditions().contains("hipertensión"));
        assertEquals(2, loaded.riskFactors().size());
    }

    @Test
    @Transactional
    void perfil_inexistente_deberiaDevolverVacio() {
        assertTrue(dashboardRepository.findProfileByUserId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @Transactional
    void reminder_deberiaGuardarYListarActivos() {
        UUID userId = UUID.randomUUID();
        Reminder reminder = Reminder.of(
                UUID.randomUUID(),
                userId,
                Reminder.ReminderType.CITA,
                "Consulta cardiología",
                OffsetDateTime.now().plusDays(7),
                true,
                OffsetDateTime.now());

        dashboardRepository.saveReminder(reminder);
        var active = dashboardRepository.findActiveRemindersByUserId(userId);

        assertFalse(active.isEmpty());
        assertEquals("Consulta cardiología", active.get(0).title());
        assertEquals(Reminder.ReminderType.CITA, active.get(0).type());
    }
}
