package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateTreatmentPlanRequest;
import com.kinplatform.kin.health.hce.dto.TreatmentPlanResponse;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
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

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class TreatmentPlanServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TreatmentPlanService treatmentPlanService;

    @Test
    void createPlan_happyPath_conEncounterValido_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter con organization_id,
        // crear TreatmentPlan con conduct y prognosis válidos, verificar persistencia.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createPlan_throwsWhenEncounterNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (encounterId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createPlan_throwsWhenConductInvalido() {
        // TODO: Verificar que conduct inválido lanza excepción.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void updatePlan_happyPath() {
        // TODO: Actualizar plan existente, verificar cambios persisten.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByEncounter_retornaListaFiltrada() {
        // TODO: Crear múltiples planes, verificar filtro por encounter.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
