package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.SurgicalHistory;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class SurgicalHistoryRepositoryTest extends PostgresTestSupport {

    @Autowired
    private SurgicalHistoryRepository repository;

    private UUID patientId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieve() {
        com.kinplatform.kin.health.hce.entity.SurgicalHistory sh = com.kinplatform.kin.health.hce.entity.SurgicalHistory.builder()
                .patientId(patientId)
                .surgeryDate(LocalDate.now().minusMonths(6))
                .procedureCupsCode("QB123")
                .procedureCupsDescription("Apendicectomía laparoscópica")
                .diagnosisCie10("K35.8")
                .surgeryType(com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType.EMERGENCY)
                .anesthesiaType(com.kinplatform.kin.health.hce.entity.SurgicalHistory.AnesthesiaType.GENERAL)
                .asaClassification(2)
                .build();

        com.kinplatform.kin.health.hce.entity.SurgicalHistory saved = repository.save(sh);

        assertThat(saved.getId()).isNotNull();

        Optional<com.kinplatform.kin.health.hce.entity.SurgicalHistory> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getProcedureCupsCode()).isEqualTo("QB123");
    }

    @Test
    void findByPatientIdOrderBySurgeryDateDesc_ShouldReturnOrdered() {
        com.kinplatform.kin.health.hce.entity.SurgicalHistory sh1 = com.kinplatform.kin.health.hce.entity.SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusYears(2))
                .procedureCupsCode("QB123").procedureCupsDescription("Apendicectomía")
                .surgeryType(com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType.EMERGENCY).build();

        com.kinplatform.kin.health.hce.entity.SurgicalHistory sh2 = com.kinplatform.kin.health.hce.entity.SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusYears(5))
                .procedureCupsCode("QB456").procedureCupsDescription("Colecistectomía")
                .surgeryType(com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType.ELECTIVE).build();

        repository.saveAll(java.util.List.of(sh1, sh2));

        List<com.kinplatform.kin.health.hce.entity.SurgicalHistory> found = repository.findByPatientIdOrderBySurgeryDateDesc(patientId);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getSurgeryDate()).isAfter(found.get(1).getSurgeryDate());
    }

    @Test
    void findByProcedureCupsCode_ShouldFind() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.SurgicalHistory sh = com.kinplatform.kin.health.hce.entity.SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusMonths(3))
                .procedureCupsCode("QB789").procedureCupsDescription("Herniorrafia")
                .surgeryType(com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType.ELECTIVE).build();

        repository.save(sh);

        List<com.kinplatform.kin.health.hce.entity.SurgicalHistory> found = repository.findByProcedureCupsCode("QB789");
        assertThat(found).hasSize(1);
    }
}


