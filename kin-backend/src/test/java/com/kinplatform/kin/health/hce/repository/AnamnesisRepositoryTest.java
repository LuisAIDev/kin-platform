package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Anamnesis;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class AnamnesisRepositoryTest extends PostgresTestSupport {

    @Autowired
    private AnamnesisRepository repository;

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
        com.kinplatform.kin.health.hce.entity.Anamnesis anamnesis = com.kinplatform.kin.health.hce.entity.Anamnesis.builder()
                .encounterId(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .physicianId(UUID.randomUUID())
                .onsetDatetime(java.time.Instant.now())
                .evolutionDescription("Evolución")
                .severitySelfReported(5)
                .build();

        com.kinplatform.kin.health.hce.entity.Anamnesis saved = repository.save(anamnesis);

        assertThat(saved.getId()).isNotNull();
        Optional<com.kinplatform.kin.health.hce.entity.Anamnesis> found = repository.findByEncounterId(saved.getEncounterId());
        assertThat(found).isPresent();
    }

    @Test
    void findByPatientId_ShouldReturnList() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.Anamnesis a1 = com.kinplatform.kin.health.hce.entity.Anamnesis.builder()
                .encounterId(UUID.randomUUID()).patientId(patientId).physicianId(UUID.randomUUID()).build();
        com.kinplatform.kin.health.hce.entity.Anamnesis a2 = com.kinplatform.kin.health.hce.entity.Anamnesis.builder()
                .encounterId(UUID.randomUUID()).patientId(patientId).physicianId(UUID.randomUUID()).build();

        repository.saveAll(java.util.List.of(a1, a2));

        List<com.kinplatform.kin.health.hce.entity.Anamnesis> found = repository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertThat(found).hasSize(2);
    }

    @Test
    void findByPhysicianId_ShouldReturnList() {
        UUID physicianId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.Anamnesis a1 = com.kinplatform.kin.health.hce.entity.Anamnesis.builder()
                .encounterId(UUID.randomUUID()).patientId(UUID.randomUUID()).physicianId(physicianId).build();
        com.kinplatform.kin.health.hce.entity.Anamnesis a2 = com.kinplatform.kin.health.hce.entity.Anamnesis.builder()
                .encounterId(UUID.randomUUID()).patientId(UUID.randomUUID()).physicianId(physicianId).build();

        repository.saveAll(java.util.List.of(a1, a2));

        List<com.kinplatform.kin.health.hce.entity.Anamnesis> found = repository.findByPhysicianIdOrderByCreatedAtDesc(physicianId);
        assertThat(found).hasSize(2);
    }
}


