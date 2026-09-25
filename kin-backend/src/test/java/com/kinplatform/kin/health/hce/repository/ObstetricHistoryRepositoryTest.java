package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.ObstetricHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObstetricHistoryRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private ObstetricHistoryRepository repository;

    @Test
    void save_ShouldPersistAndRetrieve() {
        repository.saveAndFlush(ObstetricHistory.builder()
                .patientId(patientId).gravida(2).para(1).abortions(1)
                .currentPregnancy(true).lmp(LocalDate.now().minusWeeks(20))
                .estimatedEdd(LocalDate.now().plusWeeks(20)).gestationalWeeks(20)
                .build());

        Optional<ObstetricHistory> found = repository.findByPatientId(patientId);
        assertThat(found).isPresent();
        assertThat(found.get().getGravida()).isEqualTo(2);
    }

    @Test
    void findCurrentPregnancy_ShouldReturnOnlyCurrent() {
        repository.saveAndFlush(ObstetricHistory.builder()
                .patientId(patientId).gravida(1).para(0).currentPregnancy(true)
                .lmp(LocalDate.now().minusWeeks(12)).estimatedEdd(LocalDate.now().plusWeeks(28)).build());
        repository.saveAndFlush(ObstetricHistory.builder()
                .patientId(patientId).gravida(2).para(1).currentPregnancy(false)
                .lmp(LocalDate.now().minusYears(2)).estimatedEdd(LocalDate.now().minusYears(1)).build());

        Optional<ObstetricHistory> current = repository.findCurrentPregnancyByPatientId(patientId);
        assertThat(current).isPresent();
        assertThat(current.get().getCurrentPregnancy()).isTrue();
    }
}