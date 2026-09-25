package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class PatientHistoryRepositoryTest extends PostgresTestSupport {

    @Autowired
    private PatientHistoryRepository repository;

    private UUID patientId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieveByPatientAndType() {
        com.kinplatform.kin.health.hce.entity.PatientHistory ph = com.kinplatform.kin.health.hce.entity.PatientHistory.builder()
                .patientId(UUID.randomUUID())
                .historyType(com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType.ALLERGY)
                .description("Alergia a penicilina")
                .status(com.kinplatform.kin.health.hce.entity.PatientHistory.Status.ACTIVE)
                .build();

        com.kinplatform.kin.health.hce.entity.PatientHistory saved = repository.save(ph);

        assertThat(saved.getId()).isNotNull();

        List<com.kinplatform.kin.health.hce.entity.PatientHistory> found = repository.findByPatientIdAndHistoryType(patientId, com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType.ALLERGY);
        assertThat(found).hasSize(1);
    }

    @Test
    void findByPatientIdAndStatus_ShouldFilter() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.PatientHistory ph1 = com.kinplatform.kin.health.hce.entity.PatientHistory.builder()
                .patientId(patientId)
                .historyType(com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType.ALLERGY)
                .status(com.kinplatform.kin.health.hce.entity.PatientHistory.Status.ACTIVE)
                .build();

        com.kinplatform.kin.health.hce.entity.PatientHistory ph2 = com.kinplatform.kin.health.hce.entity.PatientHistory.builder()
                .patientId(patientId)
                .historyType(com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType.SURGERY)
                .status(com.kinplatform.kin.health.hce.entity.PatientHistory.Status.RESOLVED)
                .build();

        repository.saveAll(java.util.List.of(ph1, ph2));

        List<com.kinplatform.kin.health.hce.entity.PatientHistory> active = repository.findByPatientIdAndStatus(
                patientId, com.kinplatform.kin.health.hce.entity.PatientHistory.Status.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(com.kinplatform.kin.health.hce.entity.PatientHistory.Status.ACTIVE);
    }
}


