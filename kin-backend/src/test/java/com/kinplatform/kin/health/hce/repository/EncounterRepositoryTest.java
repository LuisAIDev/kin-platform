package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EncounterRepositoryTest extends PostgresTestSupport {

    @Autowired
    private EncounterRepository repository;

    private UUID patientId;
    private UUID physicianId;
    private UUID organizationId;
    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieveById() {
        Encounter encounter = Encounter.builder()
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .appointmentId(appointmentId)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .chiefComplaint("Dolor abdominal")
                .build();

        Encounter saved = repository.save(encounter);

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
        Encounter e1 = Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(organizationId)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.COMPLETED)
                .chiefComplaint("First").build();

        Encounter e2 = Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(organizationId)
                .encounterType(Encounter.EncounterType.INPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .chiefComplaint("Second").build();

        repository.save(e1);
        repository.save(e2);

        Page<Encounter> page = repository.findByPatientIdOrderByStartedAtDesc(patientId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getStartedAt()).isAfterOrEqualTo(page.getContent().get(1).getStartedAt());
    }

    @Test
    void findByOrganizationIdAndStatus_ShouldFilterCorrectly() {
        Encounter e1 = Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(organizationId)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.COMPLETED)
                .build();

        UUID otherOrg = UUID.randomUUID();
        Encounter e2 = Encounter.builder()
                .patientId(patientId).physicianId(physicianId).organizationId(UUID.randomUUID())
                .encounterType(Encounter.EncounterType.INPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .build();

        repository.save(e1);
        repository.save(e2);

        List<Encounter> found = repository.findByOrganizationIdAndStatusOrderByStartedAtDesc(
                organizationId, Encounter.EncounterStatus.COMPLETED, PageRequest.of(0, 10)).getContent();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOrganizationId()).isEqualTo(organizationId);
        assertThat(found.get(0).getStatus()).isEqualTo(Encounter.EncounterStatus.COMPLETED);
    }

    @Test
    void organizationId_ShouldNotAcceptNull() {
        Encounter encounter = Encounter.builder()
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(null)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(encounter))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}


