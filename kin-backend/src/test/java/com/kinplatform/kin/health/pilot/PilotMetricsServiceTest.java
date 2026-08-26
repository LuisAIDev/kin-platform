package com.kinplatform.kin.health.pilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.telemedicine.InMemoryTelemedicineRepositories;
import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PilotMetricsServiceTest {

    @Test
    void report_deberiaCalcularTasasYVolumen() {
        UUID patient = UUID.randomUUID();
        UUID physician = UUID.randomUUID();
        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(PhysicianPatientAssignment.of(physician, patient, OffsetDateTime.now()));
        var triages = new InMemoryTriageConsultationRepository();
        triages.save(TriageConsultation.of(
                UUID.randomUUID(),
                patient,
                List.of("fiebre"),
                List.of(new TriageConditionResult(
                        UUID.randomUUID(),
                        "Gripe",
                        "D",
                        0.8,
                        Severity.MODERADO,
                        Urgency.MEDIA,
                        "R",
                        List.of("fiebre"))),
                OffsetDateTime.now()));
        var telemedicine = new InMemoryTelemedicineRepositories();
        telemedicine
                .messageRepository()
                .save(com.kinplatform.kin.health.telemedicine.domain.Message.of(
                        UUID.randomUUID(),
                        patient,
                        physician,
                        com.kinplatform.kin.health.telemedicine.domain.Message.conversationIdOf(patient, physician),
                        "hola",
                        false,
                        OffsetDateTime.now()));
        telemedicine
                .messageRepository()
                .save(com.kinplatform.kin.health.telemedicine.domain.Message.of(
                        UUID.randomUUID(),
                        physician,
                        patient,
                        com.kinplatform.kin.health.telemedicine.domain.Message.conversationIdOf(patient, physician),
                        "hola, cuéntame",
                        false,
                        OffsetDateTime.now().plusMinutes(30)));

        var service = new PilotMetricsService(
                triages,
                telemedicine.messageRepository(),
                telemedicine.appointmentRepository(),
                physicians.patientRepository(),
                physicians.alertRepository());

        var metrics = service.report();

        assertEquals(1, metrics.totalPatients());
        assertEquals(1, metrics.patientsWithTriage());
        assertEquals(1.0, metrics.triageCompletionRate(), 0.001);
        assertEquals(1, metrics.totalTriages());
        assertEquals(2, metrics.totalMessages());
        assertEquals(30.0, metrics.avgPhysicianResponseMinutes(), 0.001);
    }

    @Test
    void report_sinDatos_deberiaDevolverCeros() {
        var service = new PilotMetricsService(
                new InMemoryTriageConsultationRepository(),
                new InMemoryTelemedicineRepositories().messageRepository(),
                new InMemoryTelemedicineRepositories().appointmentRepository(),
                new InMemoryPhysicianRepositories().patientRepository(),
                new InMemoryPhysicianRepositories().alertRepository());

        var metrics = service.report();

        assertEquals(0, metrics.totalPatients());
        assertEquals(0.0, metrics.triageCompletionRate(), 0.001);
        assertEquals(0, metrics.totalTriages());
        assertTrue(metrics.totalMessages() == 0);
    }
}
