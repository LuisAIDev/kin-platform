package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.SurgicalHistory;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory.AnesthesiaType;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SurgicalHistoryRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private SurgicalHistoryRepository repository;

    @Test
    void save_ShouldPersistAndRetrieve() {
        SurgicalHistory sh = SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusMonths(6))
                .procedureCupsCode("QB123").procedureCupsDescription("Apendicectomía laparoscópica")
                .diagnosisCie10("K35.8").surgeryType(SurgeryType.EMERGENCY)
                .anesthesiaType(AnesthesiaType.GENERAL).asaClassification(2).build();

        SurgicalHistory saved = repository.saveAndFlush(sh);

        assertThat(saved.getId()).isNotNull();
        Optional<SurgicalHistory> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getProcedureCupsCode()).isEqualTo("QB123");
    }

    @Test
    void findByPatientIdOrderBySurgeryDateDesc_ShouldReturnOrdered() {
        repository.saveAndFlush(SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusYears(2))
                .procedureCupsCode("QB123").surgeryType(SurgeryType.EMERGENCY).build());
        repository.saveAndFlush(SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusYears(5))
                .procedureCupsCode("QB456").surgeryType(SurgeryType.ELECTIVE).build());

        List<SurgicalHistory> found = repository.findByPatientIdOrderBySurgeryDateDesc(patientId);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getSurgeryDate()).isAfter(found.get(1).getSurgeryDate());
    }

    @Test
    void findByProcedureCupsCode_ShouldFind() {
        repository.saveAndFlush(SurgicalHistory.builder()
                .patientId(patientId).surgeryDate(LocalDate.now().minusMonths(3))
                .procedureCupsCode("QB789").surgeryType(SurgeryType.ELECTIVE).build());

        List<SurgicalHistory> found = repository.findByProcedureCupsCode("QB789");
        assertThat(found).hasSize(1);
    }
}