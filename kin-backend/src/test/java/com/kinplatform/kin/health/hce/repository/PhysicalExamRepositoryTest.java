package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PhysicalExamRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private PhysicalExamRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        PhysicalExam pe = PhysicalExam.builder()
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .bpSystolic(120)
                .bpDiastolic(80)
                .heartRate(72)
                .temperature(BigDecimal.valueOf(36.5))
                .weightKg(BigDecimal.valueOf(70))
                .heightCm(BigDecimal.valueOf(170))
                .build();

        PhysicalExam saved = repository.saveAndFlush(pe);

        assertThat(saved.getId()).isNotNull();
        Optional<PhysicalExam> found = repository.findByEncounterId(encounterId);
        assertThat(found).isPresent();
        assertThat(found.get().getBpSystolic()).isEqualTo(120);
    }

    @Test
    void findByPatientIdOrderByRecordedAtDesc_ShouldReturnOrdered() {
        repository.saveAndFlush(PhysicalExam.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId)
                .bpSystolic(120).heartRate(70).build());
        repository.saveAndFlush(PhysicalExam.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId)
                .bpSystolic(130).heartRate(80).build());

        List<PhysicalExam> found = repository.findByPatientIdOrderByRecordedAtDesc(patientId);
        assertThat(found).hasSize(2);
    }
}