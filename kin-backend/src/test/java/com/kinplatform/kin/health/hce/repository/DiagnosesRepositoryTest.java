package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty;
import com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType;
import com.kinplatform.kin.health.hce.entity.Diagnoses.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosesRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private DiagnosesRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        Diagnoses dx = Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("I10").cie10Description("Hipertensión esencial")
                .diagnosisType(DiagnosisType.PRINCIPAL).certainty(Certainty.CONFIRMED)
                .status(Status.ACTIVE).build();

        Diagnoses saved = repository.saveAndFlush(dx);

        assertThat(saved.getId()).isNotNull();
        List<Diagnoses> found = repository.findByEncounterIdOrderByCreatedAtDesc(encounterId);
        assertThat(found).hasSize(1);
    }

    @Test
    void findByEncounterIdAndDiagnosisType_ShouldFindPrincipal() {
        repository.saveAndFlush(Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("I10").diagnosisType(DiagnosisType.PRINCIPAL)
                .certainty(Certainty.CONFIRMED).status(Status.ACTIVE).build());
        repository.saveAndFlush(Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("E78.5").diagnosisType(DiagnosisType.SECUNDARIO)
                .certainty(Certainty.CONFIRMED).status(Status.ACTIVE).build());

        List<Diagnoses> principals = repository.findByEncounterIdAndDiagnosisType(encounterId, DiagnosisType.PRINCIPAL);

        assertThat(principals).hasSize(1);
        assertThat(principals.get(0).getDiagnosisType()).isEqualTo(DiagnosisType.PRINCIPAL);
    }

    @Test
    void findPrincipalByEncounterId_ShouldReturnSingle() {
        repository.saveAndFlush(Diagnoses.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .cie10Code("I10").diagnosisType(DiagnosisType.PRINCIPAL)
                .certainty(Certainty.CONFIRMED).status(Status.ACTIVE).build());

        Optional<Diagnoses> principal = repository.findPrincipalByEncounterId(
                encounterId, DiagnosisType.PRINCIPAL, Status.ACTIVE);

        assertThat(principal).isPresent();
        assertThat(principal.get().getDiagnosisType()).isEqualTo(DiagnosisType.PRINCIPAL);
    }
}