package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class DischargeSummaryRepositoryTest extends PostgresTestSupport {

    @Autowired
    private DischargeSummaryRepository repository;

    private UUID patientId;
    private UUID physicianId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieve() {
        com.kinplatform.kin.health.hce.entity.DischargeSummary ds = com.kinplatform.kin.health.hce.entity.DischargeSummary.builder()
                .patientId(patientId)
                .attendingPhysicianId(physicianId)
                .admissionDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .dischargeDate(Instant.now())
                .dischargeDiagnosisCie10("I10")
                .clinicalSummary("Paciente egresado estable")
                .dischargeCondition(com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition.STABLE)
                .dischargeDisposition("Domicilio")
                .build();

        com.kinplatform.kin.health.hce.entity.DischargeSummary saved = repository.save(ds);

        assertThat(saved.getId()).isNotNull();
        Optional<com.kinplatform.kin.health.hce.entity.DischargeSummary> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByPatientId_ShouldReturnOrdered() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.DischargeSummary ds1 = com.kinplatform.kin.health.hce.entity.DischargeSummary.builder()
                .patientId(patientId).attendingPhysicianId(UUID.randomUUID())
                .admissionDate(Instant.now().minus(10, ChronoUnit.DAYS)).dischargeDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .dischargeDiagnosisCie10("I10").clinicalSummary("Egreso 1")
                .dischargeCondition(com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition.STABLE)
                .build();

        com.kinplatform.kin.health.hce.entity.DischargeSummary ds2 = com.kinplatform.kin.health.hce.entity.DischargeSummary.builder()
                .patientId(patientId).attendingPhysicianId(UUID.randomUUID())
                .admissionDate(Instant.now().minus(20, ChronoUnit.DAYS)).dischargeDate(Instant.now().minus(15, ChronoUnit.DAYS))
                .dischargeDiagnosisCie10("K59.1").clinicalSummary("Egreso 2")
                .dischargeCondition(com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition.STABLE).build();

        repository.saveAll(java.util.List.of(ds1, ds2));

        List<com.kinplatform.kin.health.hce.entity.DischargeSummary> found = repository.findByPatientIdOrderByDischargeDateDesc(patientId);
        assertThat(found).hasSize(2);
    }
}


