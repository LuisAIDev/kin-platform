package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateAnamnesisRequest;
import com.kinplatform.kin.health.hce.dto.AnamnesisResponse;
import com.kinplatform.kin.health.hce.entity.Anamnesis;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.AnamnesisRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class AnamnesisServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AnamnesisRepository anamnesisRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnamnesisService anamnesisService;

    @Test
    void createAnamnesis_happyPath_conEncounterValido_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter con organization_id,
        // crear Anamnesis y verificar persistencia con severity_self_reported entre 1-10.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createAnamnesis_throwsWhenEncounterNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (encounterId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createAnamnesis_throwsWhenSeveritySelfReportedFueraDeRango() {
        // TODO: Verificar que severitySelfReported > 10 o < 1 lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void updateAnamnesis_throwsWhenIdNotFound() {
        // TODO: Verificar que update con ID inexistente lanza EntityNotFoundException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
