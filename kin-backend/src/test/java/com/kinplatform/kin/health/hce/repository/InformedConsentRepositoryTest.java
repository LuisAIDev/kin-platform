package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType;
import com.kinplatform.kin.health.hce.entity.InformedConsent.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InformedConsentRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private InformedConsentRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveByPatientAndProcedure() {
        repository.saveAndFlush(InformedConsent.builder()
                .patientId(patientId).procedureName("Apendicectomía laparoscópica").procedureCupsCode("QB123")
                .consentType(ConsentType.SURGICAL).documentVersion("v1.0").patientSignatureHash("hash123")
                .physicianId(physicianId).physicianSignatureHash("hash456")
                .signedAt(Instant.now()).status(Status.VALID).build());

        Optional<InformedConsent> found = repository.findByPatientIdAndProcedureNameAndStatus(
                patientId, "Apendicectomía laparoscópica", Status.VALID);
        assertThat(found).isPresent();
    }

    @Test
    void findByStatus_ShouldFilter() {
        repository.saveAndFlush(InformedConsent.builder()
                .patientId(patientId).procedureName("Biopsia").consentType(ConsentType.INVASIVE)
                .documentVersion("v1").patientSignatureHash("hash1")
                .physicianId(physicianId).physicianSignatureHash("hash2")
                .signedAt(Instant.now()).status(Status.VALID).build());

        List<InformedConsent> found = repository.findByStatus(Status.VALID);
        assertThat(found).hasSize(1);
    }
}