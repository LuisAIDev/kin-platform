package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class EncounterServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private DiagnosesRepository diagnosesRepository;

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncounterService encounterService;

    @Test
    void flujoCompleto_crearEncounter_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter con organization_id,
        // verificar persistencia con status=IN_PROGRESS.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void constraintFK_crearEncounterConPacienteInexistente_lanzaExcepcion() {
        // TODO: Verificar que FK violation bloquea la inserción.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void closeEncounter_conDiagnosticoYPlan_marcaCompleted() {
        // TODO: Crear Encounter + Diagnóstico PRINCIPAL + Plan de manejo,
        // cerrar y verificar status=COMPLETED.
        // Verificar que sin diag/plan lanza IllegalStateException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
