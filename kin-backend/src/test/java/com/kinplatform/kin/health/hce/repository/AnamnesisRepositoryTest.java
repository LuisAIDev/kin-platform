package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Anamnesis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnamnesisRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private AnamnesisRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        Anamnesis anamnesis = Anamnesis.builder()
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .evolutionDescription("Evolución de prueba")
                .severitySelfReported(5)
                .build();

        Anamnesis saved = repository.saveAndFlush(anamnesis);

        assertThat(saved.getId()).isNotNull();
        Optional<Anamnesis> found = repository.findByEncounterId(encounterId);
        assertThat(found).isPresent();
    }

    @Test
    void findByPatientId_ShouldReturnList() {
        repository.saveAndFlush(Anamnesis.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId).build());
        repository.saveAndFlush(Anamnesis.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId).build());

        List<Anamnesis> found = repository.findByPatientIdOrderByCreatedAtDesc(patientId);
        assertThat(found).hasSize(2);
    }

    @Test
    void findByPhysicianId_ShouldReturnList() {
        repository.saveAndFlush(Anamnesis.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId).build());
        repository.saveAndFlush(Anamnesis.builder()
                .encounterId(newEncounter().getId()).patientId(patientId).physicianId(physicianId).build());

        List<Anamnesis> found = repository.findByPhysicianIdOrderByCreatedAtDesc(physicianId);
        assertThat(found).hasSize(2);
    }
}