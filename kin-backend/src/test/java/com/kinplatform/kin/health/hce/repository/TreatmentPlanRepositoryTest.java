package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class TreatmentPlanRepositoryTest extends PostgresTestSupport {

    @Autowired
    private TreatmentPlanRepository repository;

    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        com.kinplatform.kin.health.hce.entity.TreatmentPlan tp = com.kinplatform.kin.health.hce.entity.TreatmentPlan.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .conduct(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .prognosis(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Prognosis.GOOD)
                .estimatedDuration(java.time.Duration.ofDays(7))
                .build();

        com.kinplatform.kin.health.hce.entity.TreatmentPlan saved = repository.save(tp);

        assertThat(saved.getId()).isNotNull();
        Optional<com.kinplatform.kin.health.hce.entity.TreatmentPlan> found = repository.findByEncounterId(encounterId);
        assertThat(found).isPresent();
    }

    @Test
    void findByPatientId_ShouldReturnOrdered() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.TreatmentPlan tp1 = com.kinplatform.kin.health.hce.entity.TreatmentPlan.builder()
                .encounterId(UUID.randomUUID()).patientId(patientId).physicianId(UUID.randomUUID())
                .conduct(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .prognosis(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Prognosis.GOOD).build();

        com.kinplatform.kin.health.hce.entity.TreatmentPlan tp2 = com.kinplatform.kin.health.hce.entity.TreatmentPlan.builder()
                .encounterId(UUID.randomUUID()).patientId(patientId).physicianId(UUID.randomUUID())
                .conduct(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct.HOSPITALIZATION)
                .prognosis(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Prognosis.FAIR).build();

        repository.saveAll(java.util.List.of(tp1, tp2));

        List<com.kinplatform.kin.health.hce.entity.TreatmentPlan> found = repository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertThat(found).hasSize(2);
    }
}


