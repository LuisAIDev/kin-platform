package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class InformedConsentRepositoryTest extends PostgresTestSupport {

    @Autowired
    private InformedConsentRepository repository;

    private UUID patientId;
    private UUID physicianId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieveByPatientAndProcedure() {
        com.kinplatform.kin.health.hce.entity.InformedConsent ic = com.kinplatform.kin.health.hce.entity.InformedConsent.builder()
                .patientId(patientId)
                .procedureName("Apendicectomía laparoscópica")
                .procedureCupsCode("QB123")
                .consentType(com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType.SURGICAL)
                .documentVersion("v1.0")
                .patientSignatureHash("hash123")
                .physicianId(physicianId)
                .physicianSignatureHash("hash456")
                .signedAt(Instant.now())
                .status(com.kinplatform.kin.health.hce.entity.InformedConsent.Status.VALID)
                .build();

        com.kinplatform.kin.health.hce.entity.InformedConsent saved = repository.save(ic);

        assertThat(saved.getId()).isNotNull();

        Optional<com.kinplatform.kin.health.hce.entity.InformedConsent> found = repository.findByPatientIdAndProcedureNameAndStatus(
                patientId, "Apendicectomía laparoscópica", com.kinplatform.kin.health.hce.entity.InformedConsent.Status.VALID);
        assertThat(found).isPresent();
    }

    @Test
    void revokeConsent_ShouldUpdateStatus() {
        com.kinplatform.kin.health.hce.entity.InformedConsent ic = com.kinplatform.kin.health.hce.entity.InformedConsent.builder()
                .patientId(UUID.randomUUID())
                .procedureName("Biopsia")
                .consentType(com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType.INVASIVE)
                .documentVersion("v1")
                .patientSignatureHash("hash1")
                .physicianId(UUID.randomUUID())
                .physicianSignatureHash("hash2")
                .signedAt(Instant.now())
                .status(com.kinplatform.kin.health.hce.entity.InformedConsent.Status.VALID)
                .build();

        repository.save(ic);
        List<com.kinplatform.kin.health.hce.entity.InformedConsent> found = repository.findByStatus(com.kinplatform.kin.health.hce.entity.InformedConsent.Status.VALID);
        assertThat(found).hasSize(1);
    }
}


