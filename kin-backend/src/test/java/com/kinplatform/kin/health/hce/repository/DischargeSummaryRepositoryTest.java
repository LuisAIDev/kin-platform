package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DischargeSummaryRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private DischargeSummaryRepository repository;

    @Test
    void save_ShouldPersistAndRetrieve() {
        DischargeSummary ds = DischargeSummary.builder()
                .patientId(patientId).attendingPhysicianId(physicianId)
                .admissionDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .dischargeDate(Instant.now())
                .dischargeDiagnosisCie10("I10").clinicalSummary("Paciente egresado estable")
                .dischargeCondition(DischargeCondition.STABLE).dischargeDisposition("Domicilio")
                .build();

        DischargeSummary saved = repository.saveAndFlush(ds);

        assertThat(saved.getId()).isNotNull();
        Optional<DischargeSummary> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByPatientId_ShouldReturnOrdered() {
        repository.saveAndFlush(DischargeSummary.builder()
                .patientId(patientId).attendingPhysicianId(physicianId)
                .admissionDate(Instant.now().minus(10, ChronoUnit.DAYS)).dischargeDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .dischargeDiagnosisCie10("I10").clinicalSummary("Egreso 1")
                .dischargeCondition(DischargeCondition.STABLE).build());
        repository.saveAndFlush(DischargeSummary.builder()
                .patientId(patientId).attendingPhysicianId(physicianId)
                .admissionDate(Instant.now().minus(20, ChronoUnit.DAYS)).dischargeDate(Instant.now().minus(15, ChronoUnit.DAYS))
                .dischargeDiagnosisCie10("K59.1").clinicalSummary("Egreso 2")
                .dischargeCondition(DischargeCondition.STABLE).build());

        List<DischargeSummary> found = repository.findByPatientIdOrderByDischargeDateDesc(patientId);
        assertThat(found).hasSize(2);
    }
}