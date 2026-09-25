package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Prognosis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TreatmentPlanRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private TreatmentPlanRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        repository.saveAndFlush(TreatmentPlan.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .conduct(Conduct.OUTPATIENT_TREATMENT).prognosis(Prognosis.GOOD)
                .estimatedDuration(Duration.ofDays(7))
                .build());

        Optional<TreatmentPlan> found = repository.findByEncounterId(encounterId);
        assertThat(found).isPresent();
        assertThat(found.get().getConduct()).isEqualTo(Conduct.OUTPATIENT_TREATMENT);
    }

    @Test
    void findByPatientId_ShouldReturnOrdered() {
        repository.saveAndFlush(TreatmentPlan.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .conduct(Conduct.OUTPATIENT_TREATMENT).prognosis(Prognosis.GOOD).build());
        repository.saveAndFlush(TreatmentPlan.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId)
                .conduct(Conduct.HOSPITALIZATION).prognosis(Prognosis.FAIR).build());

        List<TreatmentPlan> found = repository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertThat(found).hasSize(2);
    }
}