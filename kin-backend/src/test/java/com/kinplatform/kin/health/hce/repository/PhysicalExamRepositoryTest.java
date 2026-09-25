package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class PhysicalExamRepositoryTest extends PostgresTestSupport {

    @Autowired
    private PhysicalExamRepository repository;

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
        com.kinplatform.kin.health.hce.entity.PhysicalExam pe = com.kinplatform.kin.health.hce.entity.PhysicalExam.builder()
                .encounterId(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .physicianId(UUID.randomUUID())
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .temperature(java.math.BigDecimal.valueOf(36.5))
                .build();

        com.kinplatform.kin.health.hce.entity.PhysicalExam saved = repository.save(pe);

        assertThat(saved.getId()).isNotNull();
        Optional<com.kinplatform.kin.health.hce.entity.PhysicalExam> found = repository.findByEncounterId(saved.getEncounterId());
        assertThat(found).isPresent();
    }

    @Test
    void findByPatientIdOrderByRecordedAtDesc_ShouldReturnOrdered() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.PhysicalExam pe1 = com.kinplatform.kin.health.hce.entity.PhysicalExam.builder()
                .encounterId(UUID.randomUUID()).patientId(patientId).physicianId(UUID.randomUUID())
                .bpSystolic(120).heartRate(70).build();

        com.kinplatform.kin.health.hce.entity.PhysicalExam pe2 = com.kinplatform.kin.health.hce.entity.PhysicalExam.builder()
                .encounterId(UUID.randomUUID()).patientId(patientId).physicianId(UUID.randomUUID())
                .bpSystolic(130).heartRate(80).build();

        repository.saveAll(java.util.List.of(pe1, pe2));

        List<com.kinplatform.kin.health.hce.entity.PhysicalExam> found = repository.findByPatientIdOrderByRecordedAtDesc(patientId);
        assertThat(found).hasSize(2);
    }
}


