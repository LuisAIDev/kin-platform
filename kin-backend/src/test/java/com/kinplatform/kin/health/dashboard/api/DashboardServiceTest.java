package com.kinplatform.kin.health.dashboard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.dashboard.InMemoryDashboardRepository;
import com.kinplatform.kin.health.dashboard.config.DashboardProperties;
import com.kinplatform.kin.health.dashboard.domain.CareRecommendationRegistry;
import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class DashboardServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010002");
    private static final UUID C2 = UUID.fromString("22220000-0000-0000-0000-000000010032");

    private static DashboardProperties properties(boolean enabled) {
        var props = new DashboardProperties();
        props.setEnabled(enabled);
        props.setTopConditions(3);
        return props;
    }

    private static TriageConsultation consultation(String symptom, String conditionName, UUID conditionId) {
        return TriageConsultation.of(
                UUID.randomUUID(),
                USER_ID,
                List.of(symptom),
                List.of(new TriageConditionResult(
                        conditionId, conditionName, "D", 0.8, Severity.MODERADO, Urgency.MEDIA, "R", List.of(symptom))),
                OffsetDateTime.now());
    }

    private static DashboardService service(
            InMemoryTriageConsultationRepository consultations, InMemoryDashboardRepository dashboard) {
        return new DashboardService(dashboard, consultations, CareRecommendationRegistry.defaults(), properties(true));
    }

    @Test
    void summary_deberiaCalcularEstadisticas() {
        var consultations = new InMemoryTriageConsultationRepository();
        consultations.save(consultation("fiebre", "Gripe", C1));
        consultations.save(consultation("tos", "Gripe", C1));
        consultations.save(consultation("mareo", "Hipertensión arterial", C2));
        var service = service(consultations, new InMemoryDashboardRepository());

        var summary = service.summary(USER_ID);

        assertEquals(3, summary.totalConsultations());
        assertEquals(0, summary.totalDifferentials());
        assertFalse(summary.topConditions().isEmpty());
        assertEquals("Gripe", summary.topConditions().get(0).name());
        assertEquals(2, summary.topConditions().get(0).occurrences());
        assertNotNull(summary.lastTriageAt());
    }

    @Test
    void summary_conConsultasDeOtroUsuario_deberiaAislar() {
        var consultations = new InMemoryTriageConsultationRepository();
        var other = TriageConsultation.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of("x"),
                List.of(new TriageConditionResult(
                        C1, "Gripe", "D", 0.8, Severity.MODERADO, Urgency.MEDIA, "R", List.of("x"))),
                OffsetDateTime.now());
        consultations.save(other);
        var service = service(consultations, new InMemoryDashboardRepository());

        var summary = service.summary(USER_ID);

        assertEquals(0, summary.totalConsultations());
        assertTrue(summary.topConditions().isEmpty());
    }

    @Test
    void history_deberiaDevolverPagina() {
        var consultations = new InMemoryTriageConsultationRepository();
        for (int i = 0; i < 5; i++) {
            consultations.save(consultation("fiebre", "Gripe", C1));
        }
        var service = service(consultations, new InMemoryDashboardRepository());

        var page = service.history(USER_ID, PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }

    @Test
    void consultationDetail_deberiaDevolverLaConsultaDelUsuario() {
        var consultations = new InMemoryTriageConsultationRepository();
        var c = consultation("fiebre", "Gripe", C1);
        consultations.save(c);
        var service = service(consultations, new InMemoryDashboardRepository());

        var detail = service.consultationDetail(USER_ID, c.id());

        assertEquals(c.id(), detail.id());
        assertEquals(List.of("fiebre"), detail.symptoms());
    }

    @Test
    void consultationDetail_deOtroUsuario_deberiaLanzar() {
        var consultations = new InMemoryTriageConsultationRepository();
        var other = TriageConsultation.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of("x"),
                List.of(new TriageConditionResult(
                        C1, "Gripe", "D", 0.8, Severity.MODERADO, Urgency.MEDIA, "R", List.of("x"))),
                OffsetDateTime.now());
        consultations.save(other);
        var service = service(consultations, new InMemoryDashboardRepository());

        assertThrows(
                DashboardConsultationNotFoundException.class, () -> service.consultationDetail(USER_ID, other.id()));
    }

    @Test
    void updateProfile_deberiaGuardarYRecuperar() {
        var dashboard = new InMemoryDashboardRepository();
        var service = service(new InMemoryTriageConsultationRepository(), dashboard);

        var saved = service.updateProfile(USER_ID, List.of("fumador", "diabetes"), List.of("hipertensión"));
        var loaded = service.profile(USER_ID);

        assertTrue(loaded.riskFactors().contains("fumador"));
        assertTrue(loaded.riskFactors().contains("diabetes"));
        assertTrue(loaded.chronicConditions().contains("hipertensión"));
        assertEquals(USER_ID, saved.userId());
    }

    @Test
    void profile_sinPerfil_deberiaDevolverVacio() {
        var service = service(new InMemoryTriageConsultationRepository(), new InMemoryDashboardRepository());

        var profile = service.profile(USER_ID);

        assertEquals(USER_ID, profile.userId());
        assertTrue(profile.riskFactors().isEmpty());
    }

    @Test
    void carePlan_deberiaCombinarPerfilYHistorial() {
        var consultations = new InMemoryTriageConsultationRepository();
        consultations.save(consultation("mareo", "Hipertensión arterial", C2));
        var dashboard = new InMemoryDashboardRepository();
        dashboard.saveProfile(
                PatientProfile.of(USER_ID, List.of("fumador"), List.of("diabetes"), OffsetDateTime.now()));
        var service = service(consultations, dashboard);

        var plan = service.carePlan(USER_ID);

        assertFalse(plan.recommendations().isEmpty());
        assertTrue(plan.sourceConditions().contains("diabetes"));
        assertTrue(plan.sourceConditions().contains("Hipertensión arterial"));
        assertTrue(plan.recommendations().stream().anyMatch(r -> r.contains("glucosa")));
    }

    @Test
    void carePlan_sinCondiciones_deberiaSerVacio() {
        var service = service(new InMemoryTriageConsultationRepository(), new InMemoryDashboardRepository());

        var plan = service.carePlan(USER_ID);

        assertTrue(plan.recommendations().isEmpty());
    }

    @Test
    void createReminder_deberiaGuardarConUserIdDelUsuario() {
        var dashboard = new InMemoryDashboardRepository();
        var service = service(new InMemoryTriageConsultationRepository(), dashboard);
        var reminder = Reminder.of(
                UUID.randomUUID(),
                USER_ID,
                Reminder.ReminderType.CITA,
                "Consulta cardiología",
                OffsetDateTime.now().plusDays(7),
                true,
                null);

        var saved = service.createReminder(USER_ID, reminder);
        var reminders = service.reminders(USER_ID);

        assertEquals(USER_ID, saved.userId());
        assertEquals(1, reminders.size());
        assertEquals("Consulta cardiología", reminders.get(0).title());
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        var props = properties(false);
        var service = new DashboardService(
                new InMemoryDashboardRepository(),
                new InMemoryTriageConsultationRepository(),
                CareRecommendationRegistry.defaults(),
                props);

        assertThrows(DashboardDisabledException.class, () -> service.summary(USER_ID));
        assertThrows(DashboardDisabledException.class, () -> service.history(USER_ID, PageRequest.of(0, 10)));
        assertThrows(DashboardDisabledException.class, () -> service.profile(USER_ID));
        assertThrows(DashboardDisabledException.class, () -> service.carePlan(USER_ID));
    }
}
