package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class DiagnosesRepositoryTest extends PostgresTestSupport {

    @Autowired
    private DiagnosesRepository repository;

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
        com.kinplatform.kin.health.hce.entity.Diagnoses dx = com.kinplatform.kin.health.hce.entity.Diagnoses.builder()
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("I10")
                .cie10Description("Hipertensión esencial")
                .diagnosisType(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty.CONFIRMED)
                .status(com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE)
                .build();

        com.kinplatform.kin.health.hce.entity.Diagnoses saved = repository.save(dx);

        assertThat(saved.getId()).isNotNull();

        List<com.kinplatform.kin.health.hce.entity.Diagnoses> found = repository.findByEncounterIdOrderByCreatedAtDesc(encounterId);
        assertThat(found).hasSize(1);
    }

    @Test
    void findByEncounterIdAndDiagnosisType_ShouldFindPrincipal() {
        com.kinplatform.kin.health.hce.entity.Diagnoses dx1 = com.kinplatform.kin.health.hce.entity.Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("I10").diagnosisType(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty.CONFIRMED).status(com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE)
                .build();

        com.kinplatform.kin.health.hce.entity.Diagnoses dx2 = com.kinplatform.kin.health.hce.entity.Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("E78.5").diagnosisType(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.SECUNDARIO)
                .certainty(com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty.CONFIRMED).status(com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE)
                .build();

        repository.saveAll(java.util.List.of(dx1, dx2));

        List<com.kinplatform.kin.health.hce.entity.Diagnoses> principals = repository.findByEncounterIdAndDiagnosisType(
                encounterId, com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL);

        assertThat(principals).hasSize(1);
        assertThat(principals.get(0).getDiagnosisType()).isEqualTo(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL);
    }

    @Test
    void findPrincipalByEncounterId_ShouldReturnSingle() {
        com.kinplatform.kin.health.hce.entity.Diagnoses dx = com.kinplatform.kin.health.hce.entity.Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("I10").diagnosisType(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty.CONFIRMED).status(com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE)
                .build();

        repository.save(dx);

        Optional<com.kinplatform.kin.health.hce.entity.Diagnoses> principal = repository.findPrincipalByEncounterId(
                encounterId, com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL, com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE);

        assertThat(principal).isPresent();
        assertThat(principal.get().getDiagnosisType()).isEqualTo(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL);
    }
}


