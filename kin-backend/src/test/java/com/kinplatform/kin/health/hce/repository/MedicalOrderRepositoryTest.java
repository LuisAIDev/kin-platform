package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.MedicalOrder;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class MedicalOrderRepositoryTest extends PostgresTestSupport {

    @Autowired
    private MedicalOrderRepository repository;

    private UUID treatmentPlanId;
    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        treatmentPlanId = UUID.randomUUID();
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieveByEncounterId() {
        com.kinplatform.kin.health.hce.entity.MedicalOrder mo = com.kinplatform.kin.health.hce.entity.MedicalOrder.builder()
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType.MEDICATION)
                .drugName("Ibuprofeno")
                .dose("400mg")
                .route(com.kinplatform.kin.health.hce.entity.MedicalOrder.Route.ORAL)
                .frequency("Cada 8 horas")
                .priority(com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority.ROUTINE)
                .status(com.kinplatform.kin.health.hce.entity.MedicalOrder.Status.ORDERED)
                .build();

        com.kinplatform.kin.health.hce.entity.MedicalOrder saved = repository.save(mo);

        assertThat(saved.getId()).isNotNull();
        List<com.kinplatform.kin.health.hce.entity.MedicalOrder> found = repository.findByEncounterIdOrderByOrderedAtDescList(encounterId);
        assertThat(found).hasSize(1);
    }

    @Test
    void findByTreatmentPlanId_ShouldReturnOrdered() {
        UUID treatmentPlanId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.MedicalOrder mo1 = com.kinplatform.kin.health.hce.entity.MedicalOrder.builder()
                .treatmentPlanId(treatmentPlanId).encounterId(UUID.randomUUID()).patientId(UUID.randomUUID())
                .physicianId(UUID.randomUUID()).orderType(com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType.MEDICATION)
                .drugName("Paracetamol").dose("500mg").route(com.kinplatform.kin.health.hce.entity.MedicalOrder.Route.ORAL)
                .frequency("Cada 6h").priority(com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority.ROUTINE)
                .status(com.kinplatform.kin.health.hce.entity.MedicalOrder.Status.ORDERED).build();

        com.kinplatform.kin.health.hce.entity.MedicalOrder mo2 = com.kinplatform.kin.health.hce.entity.MedicalOrder.builder()
                .treatmentPlanId(treatmentPlanId).encounterId(UUID.randomUUID()).patientId(UUID.randomUUID())
                .physicianId(UUID.randomUUID()).orderType(com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType.LAB_EXAM)
                .cupsCode("900123").priority(com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority.URGENT)
                .status(com.kinplatform.kin.health.hce.entity.MedicalOrder.Status.ORDERED).build();

        repository.saveAll(java.util.List.of(mo1, mo2));

        List<com.kinplatform.kin.health.hce.entity.MedicalOrder> found = repository.findByTreatmentPlanIdOrderByOrderedAtDescList(treatmentPlanId);
        assertThat(found).hasSize(2);
    }
}


