package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.ObstetricHistory;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class ObstetricHistoryRepositoryTest extends PostgresTestSupport {

    @Autowired
    private ObstetricHistoryRepository repository;

    private UUID patientId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieve() {
        com.kinplatform.kin.health.hce.entity.ObstetricHistory oh = com.kinplatform.kin.health.hce.entity.ObstetricHistory.builder()
                .patientId(patientId)
                .gravida(2).para(1).abortions(1)
                .currentPregnancy(true)
                .lmp(LocalDate.now().minusWeeks(20))
                .estimatedEdd(LocalDate.now().plusWeeks(20))
                .gestationalWeeks(20)
                .build();

        com.kinplatform.kin.health.hce.entity.ObstetricHistory saved = repository.save(oh);

        assertThat(saved.getId()).isNotNull();

        Optional<com.kinplatform.kin.health.hce.entity.ObstetricHistory> found = repository.findByPatientId(patientId);
        assertThat(found).isPresent();
        assertThat(found.get().getGravida()).isEqualTo(2);
    }

    @Test
    void findCurrentPregnancy_ShouldReturnOnlyCurrent() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.ObstetricHistory oh1 = com.kinplatform.kin.health.hce.entity.ObstetricHistory.builder()
                .patientId(patientId).gravida(1).para(0).currentPregnancy(true)
                .lmp(LocalDate.now().minusWeeks(12)).estimatedEdd(LocalDate.now().plusWeeks(28))
                .build();

        com.kinplatform.kin.health.hce.entity.ObstetricHistory oh2 = com.kinplatform.kin.health.hce.entity.ObstetricHistory.builder()
                .patientId(patientId).gravida(2).para(1).currentPregnancy(false)
                .lmp(LocalDate.now().minusYears(2)).estimatedEdd(LocalDate.now().minusYears(1))
                .build();

        repository.saveAll(java.util.List.of(oh1, oh2));

        Optional<com.kinplatform.kin.health.hce.entity.ObstetricHistory> current = repository.findCurrentPregnancyByPatientId(patientId);
        assertThat(current).isPresent();
        assertThat(current.get().getCurrentPregnancy()).isTrue();
    }
}


