package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Route;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Status;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalOrderRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private MedicalOrderRepository repository;

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;

    private UUID planId;

    @BeforeEach
    void createPlan() {
        planId = treatmentPlanRepository.saveAndFlush(TreatmentPlan.builder()
                .encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .build()).getId();
    }

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        repository.saveAndFlush(MedicalOrder.builder()
                .treatmentPlanId(planId).encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .orderType(OrderType.MEDICATION).drugName("Ibuprofeno").dose("400mg")
                .route(Route.ORAL).frequency("Cada 8 horas").priority(Priority.ROUTINE).status(Status.ORDERED)
                .build());

        List<MedicalOrder> found = repository.findByEncounterIdOrderByOrderedAtDescList(encounterId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getDrugName()).isEqualTo("Ibuprofeno");
    }

    @Test
    void findByTreatmentPlanId_ShouldReturnOrdered() {
        repository.saveAndFlush(MedicalOrder.builder()
                .treatmentPlanId(planId).encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .orderType(OrderType.MEDICATION).drugName("Paracetamol").dose("500mg")
                .route(Route.ORAL).frequency("Cada 6h").priority(Priority.ROUTINE).status(Status.ORDERED).build());
        repository.saveAndFlush(MedicalOrder.builder()
                .treatmentPlanId(planId).encounterId(encounterId).patientId(patientId).physicianId(physicianId)
                .orderType(OrderType.LAB_EXAM).cupsCode("900123").priority(Priority.URGENT)
                .status(Status.ORDERED).build());

        List<MedicalOrder> found = repository.findByTreatmentPlanIdOrderByOrderedAtDescList(planId);
        assertThat(found).hasSize(2);
    }
}