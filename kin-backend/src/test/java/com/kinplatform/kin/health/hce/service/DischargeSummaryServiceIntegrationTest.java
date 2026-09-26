package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateDischargeSummaryRequest;
import com.kinplatform.kin.health.hce.dto.DischargeSummaryResponse;
import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.kin.health.hce.repository.DischargeSummaryRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
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
class DischargeSummaryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DischargeSummaryRepository dischargeSummaryRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DischargeSummaryService dischargeSummaryService;

    @Test
    void createDischargeSummary_happyPath_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter con organization_id,
        // crear DischargeSummary con admissionDate < dischargeDate y dischargeDiagnosisCie10,
        // verificar persistencia con lengthOfStay calculado automáticamente.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createDischargeSummary_throwsWhenEncounterNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (encounterId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createDischargeSummary_throwsWhenDischargeDateBeforeAdmissionDate() {
        // TODO: Verificar que dischargeDate < admissionDate lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createDischargeSummary_throwsWhenDischargeDiagnosisCie10Null() {
        // TODO: Verificar que dischargeDiagnosisCie10 null lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void signDischargeSummary_flujoCompleto_firmaYVerifica() {
        // TODO: Crear DischargeSummary sin firmar, firmar con physicianId correcto,
        // verificar status firmado, signedAt y physicianSignatureHash.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void signDischargeSummary_throwsWhenPhysicianIdMismatch() {
        // TODO: Verificar que firmar con physicianId distinto al attending lanza AccessDeniedException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void signDischargeSummary_throwsWhenAlreadySigned() {
        // TODO: Verificar que firmar dos veces lanza IllegalStateException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}