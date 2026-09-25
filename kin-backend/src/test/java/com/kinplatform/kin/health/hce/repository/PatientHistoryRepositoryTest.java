package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatientHistoryRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private PatientHistoryRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveByPatientAndType() {
        repository.saveAndFlush(PatientHistory.builder()
                .patientId(patientId)
                .historyType(HistoryType.ALLERGY)
                .description("Alergia a penicilina")
                .status(Status.ACTIVE)
                .build());

        List<PatientHistory> found = repository.findByPatientIdAndHistoryType(patientId, HistoryType.ALLERGY);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getDescription()).isEqualTo("Alergia a penicilina");
    }

    @Test
    void findByPatientIdAndStatus_ShouldFilter() {
        repository.saveAndFlush(PatientHistory.builder()
                .patientId(patientId).historyType(HistoryType.ALLERGY)
                .description("Alergia").status(Status.ACTIVE).build());
        repository.saveAndFlush(PatientHistory.builder()
                .patientId(patientId).historyType(HistoryType.SURGERY)
                .description("Cirugía previa").status(Status.RESOLVED).build());

        List<PatientHistory> active = repository.findByPatientIdAndStatus(patientId, Status.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(Status.ACTIVE);
    }
}