package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateSurgicalHistoryRequest;
import com.kinplatform.kin.health.hce.dto.SurgicalHistoryResponse;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory;
import com.kinplatform.kin.health.hce.repository.SurgicalHistoryRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class SurgicalHistoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SurgicalHistoryRepository surgicalHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SurgicalHistoryService surgicalHistoryService;

    @Test
    void addSurgery_happyPath_verificarEnBD() {
        // TODO: Crear User paciente, agregar Surgery con CUPS válido y anestesia GENERAL,
        // verificar persistencia con FK a patient_id y asa_classification.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addSurgery_throwsWhenPatientNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (patientId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addSurgery_throwsWhenProcedureCupsCodeNull() {
        // TODO: Verificar que procedureCupsCode null lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addSurgery_throwsWhenSurgeryDateFuturo() {
        // TODO: Verificar que surgeryDate futuro lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addSurgery_throwsWhenAsaClassificationInvalido() {
        // TODO: Verificar que asaClassification > 6 o < 1 lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByPatient_retornaListaFiltrada() {
        // TODO: Crear múltiples cirugías, verificar filtro por patientId.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByCupsCode_retornaFiltrado() {
        // TODO: Crear múltiples cirugías con mismo CUPS, verificar filtro por procedureCupsCode.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}