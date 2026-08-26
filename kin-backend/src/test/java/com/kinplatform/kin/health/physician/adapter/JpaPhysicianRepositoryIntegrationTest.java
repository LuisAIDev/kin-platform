package com.kinplatform.kin.health.physician.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.test.PostgresTestSupport;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de los adaptadores JPA del portal de médicos con PostgreSQL real
 * (Testcontainers). Verifica que Flyway V25 crea las tablas y que las
 * asignaciones y alertas persisten/consultas correctamente.
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaPhysicianRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    private JpaPhysicianPatientRepository patientRepository;

    @Autowired
    private JpaClinicalAlertRepository alertRepository;

    @Test
    @Transactional
    void asignacion_deberiaGuardarYConsultar() {
        UUID physicianId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        patientRepository.assign(PhysicianPatientAssignment.of(physicianId, patientId, OffsetDateTime.now()));

        assertTrue(patientRepository.isAssigned(physicianId, patientId));
        assertEquals(1, patientRepository.findPatientIdsByPhysician(physicianId).size());
        assertEquals(
                patientId,
                patientRepository.findPatientIdsByPhysician(physicianId).get(0));
        assertEquals(1, patientRepository.findPhysicianIdsByPatient(patientId).size());
        assertEquals(
                physicianId,
                patientRepository.findPhysicianIdsByPatient(patientId).get(0));
    }

    @Test
    @Transactional
    void asignacion_deberiaRechazarPacienteNoAsignado() {
        assertFalse(patientRepository.isAssigned(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    @Transactional
    void alerta_deberiaGuardarYListarActivas() {
        UUID physicianId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ClinicalAlert alert = ClinicalAlert.of(
                UUID.randomUUID(),
                patientId,
                physicianId,
                ClinicalAlert.AlertType.HIGH_URGENCY_TRIAGE,
                ClinicalAlert.AlertSeverity.ALTA,
                "Triaje de alta urgencia: Angina de pecho",
                ClinicalAlert.AlertStatus.PENDING,
                OffsetDateTime.now(),
                null);

        alertRepository.save(alert);
        var active = alertRepository.findActiveByPhysician(physicianId);

        assertEquals(1, active.size());
        assertEquals(ClinicalAlert.AlertStatus.PENDING, active.get(0).status());
        assertEquals(patientId, active.get(0).patientId());
    }

    @Test
    @Transactional
    void alertaAcknowledged_deberiaNoAparecerComoActiva() {
        UUID physicianId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ClinicalAlert alert = ClinicalAlert.of(
                UUID.randomUUID(),
                patientId,
                physicianId,
                ClinicalAlert.AlertType.HIGH_URGENCY_TRIAGE,
                ClinicalAlert.AlertSeverity.ALTA,
                "mensaje",
                ClinicalAlert.AlertStatus.ACKNOWLEDGED,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        alertRepository.save(alert);

        assertTrue(alertRepository.findActiveByPhysician(physicianId).isEmpty());
    }
}
