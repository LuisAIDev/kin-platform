package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.entity.Referral.Priority;
import com.kinplatform.kin.health.hce.entity.Referral.ReferralType;
import com.kinplatform.kin.health.hce.entity.Referral.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private ReferralRepository repository;

    @Test
    void save_ShouldPersistAndRetrieve() {
        Referral r = Referral.builder()
                .patientId(patientId).referringPhysicianId(physicianId)
                .referredToService("Cardiología").referredToInstitution("Hospital Central")
                .referralType(ReferralType.INTERCONSULTATION).priority(Priority.ROUTINE)
                .reason("Dolor torácico").clinicalSummary("Paciente con dolor precordial")
                .status(Status.PENDING).build();

        Referral saved = repository.saveAndFlush(r);

        assertThat(saved.getId()).isNotNull();
        Optional<Referral> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByStatus_ShouldFilter() {
        repository.saveAndFlush(Referral.builder()
                .patientId(patientId).referringPhysicianId(physicianId)
                .referredToService("Neurología").referredToInstitution("Hospital A")
                .referralType(ReferralType.INTERCONSULTATION).priority(Priority.URGENT)
                .reason("Cefalea").status(Status.PENDING).build());
        repository.saveAndFlush(Referral.builder()
                .patientId(patientId).referringPhysicianId(physicianId)
                .referredToService("Cardiología").referredToInstitution("Hospital B")
                .referralType(ReferralType.INTERCONSULTATION).priority(Priority.ROUTINE)
                .reason("Dolor torácico").status(Status.ACCEPTED).build());

        List<Referral> pending = repository.findByStatus(Status.PENDING);
        assertThat(pending).hasSize(1);
    }
}