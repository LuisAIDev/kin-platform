package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreatePhysicalExamRequest;
import com.kinplatform.kin.health.hce.dto.PhysicalExamResponse;
import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.PhysicalExamRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class PhysicalExamServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PhysicalExamRepository physicalExamRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhysicalExamService physicalExamService;

    @Test
    void recordExam_happyPath_conEncounterValido_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter con organization_id,
        // registrar PhysicalExam con signos vitales válidos, verificar persistencia y BMI calculado.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void recordExam_throwsWhenEncounterNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (encounterId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void recordExam_throwsWhenSignosVitalesFueraDeRango() {
        // TODO: Verificar que bpSystolic > 300, heartRate < 30, temperature > 45, spo2 < 50, glasgowScore > 15 lanzan IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void recordExam_calculaBmiCorrectamente() {
        // TODO: Verificar BMI = weight / (height/100)^2 con weight=70kg, height=175cm → 22.86.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
