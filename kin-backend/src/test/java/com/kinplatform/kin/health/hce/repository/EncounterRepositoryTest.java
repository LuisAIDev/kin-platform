package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private EncounterRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveById() {
        Encounter encounter = Encounter.builder()
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .chiefComplaint("Dolor abdominal")
                .build();

        Encounter saved = repository.saveAndFlush(encounter);

        assertThat(saved.getId()).isNotNull();
        Optional<Encounter> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPatientId()).isEqualTo(patientId);
        assertThat(found.get().getChiefComplaint()).isEqualTo("Dolor abdominal");
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        Optional<Encounter> found = repository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findByPatientIdOrderByStartedAtDesc_ShouldReturnOrdered() {
        // Base ya sembró 1 encounter para patientId.
        repository.saveAndFlush(Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(organizationId)
                .encounterType(EncounterType.OUTPATIENT).status(EncounterStatus.COMPLETED)
                .chiefComplaint("First").build());
        repository.saveAndFlush(Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(organizationId)
                .encounterType(EncounterType.INPATIENT).status(EncounterStatus.COMPLETED)
                .chiefComplaint("Second").build());

        Page<Encounter> page = repository.findByPatientIdOrderByStartedAtDesc(patientId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getStartedAt())
                .isAfterOrEqualTo(page.getContent().get(2).getStartedAt());
    }

    @Test
    void findByOrganizationIdAndStatus_ShouldFilterCorrectly() {
        repository.saveAndFlush(Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(organizationId)
                .encounterType(EncounterType.OUTPATIENT).status(EncounterStatus.COMPLETED).build());
        repository.saveAndFlush(Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(UUID.randomUUID())
                .encounterType(EncounterType.INPATIENT).status(EncounterStatus.COMPLETED).build());

        List<Encounter> found = repository.findByOrganizationIdAndStatusOrderByStartedAtDesc(
                organizationId, EncounterStatus.COMPLETED, PageRequest.of(0, 10)).getContent();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOrganizationId()).isEqualTo(organizationId);
        assertThat(found.get(0).getStatus()).isEqualTo(EncounterStatus.COMPLETED);
    }

    @Test
    void organizationId_ShouldNotAcceptNull() {
        Encounter encounter = Encounter.builder()
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(null)
                .encounterType(EncounterType.OUTPATIENT)
                .status(EncounterStatus.IN_PROGRESS)
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(encounter))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}