package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateMedicalOrderRequest;
import com.kinplatform.kin.health.hce.dto.MedicalOrderResponse;
import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.MedicalOrderRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class MedicalOrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MedicalOrderRepository medicalOrderRepository;

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicalOrderService medicalOrderService;

    @Test
    void addOrder_happyPath_conTreatmentPlanValido_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter + TreatmentPlan,
        // agregar MedicalOrder (LAB_EXAM y MEDICATION), verificar persistencia con FK a treatment_plans.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addOrder_throwsWhenTreatmentPlanNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (treatmentPlanId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void executeOrder_flujoCompleto_ordenadoAEjecutado() {
        // TODO: Crear orden ORDERED, ejecutar, verificar status=EXECUTED, executedAt, executedBy.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void cancelOrder_flujoCompleto_ordenadoACancelado() {
        // TODO: Crear orden ORDERED, cancelar, verificar status=CANCELLED, executionNotes.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByTreatmentPlan_retornaOrdenesDelPlan() {
        // TODO: Crear múltiples órdenes para un plan, verificar filtro por treatmentPlanId.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
