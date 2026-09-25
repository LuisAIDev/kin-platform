package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Referral;
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
class ReferralRepositoryTest extends PostgresTestSupport {

    @Autowired
    private ReferralRepository repository;

    private UUID patientId;
    private UUID physicianId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieve() {
        com.kinplatform.kin.health.hce.entity.Referral r = com.kinplatform.kin.health.hce.entity.Referral.builder()
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referredToService("Cardiología")
                .referredToInstitution("Hospital Central")
                .referralType(com.kinplatform.kin.health.hce.entity.Referral.ReferralType.INTERCONSULTATION)
                .priority(com.kinplatform.kin.health.hce.entity.Referral.Priority.ROUTINE)
                .reason("Dolor torácico")
                .clinicalSummary("Paciente con dolor precordial")
                .status(com.kinplatform.kin.health.hce.entity.Referral.Status.PENDING)
                .build();

        com.kinplatform.kin.health.hce.entity.Referral saved = repository.save(r);

        assertThat(saved.getId()).isNotNull();
        Optional<com.kinplatform.kin.health.hce.entity.Referral> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByStatus_ShouldFilter() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.Referral r1 = com.kinplatform.kin.health.hce.entity.Referral.builder()
                .patientId(patientId).referringPhysicianId(UUID.randomUUID())
                .referredToService("Neurología").referredToInstitution("Hospital A")
                .referralType(com.kinplatform.kin.health.hce.entity.Referral.ReferralType.INTERCONSULTATION)
                .priority(com.kinplatform.kin.health.hce.entity.Referral.Priority.URGENT)
                .reason("Cefalea").status(com.kinplatform.kin.health.hce.entity.Referral.Status.PENDING).build();

        com.kinplatform.kin.health.hce.entity.Referral r2 = com.kinplatform.kin.health.hce.entity.Referral.builder()
                .patientId(patientId).referringPhysicianId(UUID.randomUUID())
                .referredToService("Cardiología").referredToInstitution("Hospital B")
                .referralType(com.kinplatform.kin.health.hce.entity.Referral.ReferralType.INTERCONSULTATION)
                .priority(com.kinplatform.kin.health.hce.entity.Referral.Priority.ROUTINE)
                .reason("Dolor torácico").status(com.kinplatform.kin.health.hce.entity.Referral.Status.ACCEPTED).build();

        repository.saveAll(java.util.List.of(r1, r2));

        List<com.kinplatform.kin.health.hce.entity.Referral> pending = repository.findByStatus(com.kinplatform.kin.health.hce.entity.Referral.Status.PENDING);
        assertThat(pending).hasSize(1);
    }
}


