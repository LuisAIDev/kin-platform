package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreatePatientHistoryRequest;
import com.kinplatform.kin.health.hce.dto.PatientHistoryResponse;
import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.repository.PatientHistoryRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class PatientHistoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PatientHistoryRepository patientHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientHistoryService patientHistoryService;

    @Test
    void addHistory_happyPath_allergy_y_surgery_verificarEnBD() {
        // TODO: Crear User paciente, agregar history ALLERGY y SURGERY,
        // verificar persistencia con status=ACTIVE/RESOLVED y history_type correcto.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addHistory_throwsWhenPatientNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (patientId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addHistory_throwsWhenHistoryTypeInvalido() {
        // TODO: Verificar que historyType null/inválido lanza excepción.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByPatientAndType_retornaListaFiltrada() {
        // TODO: Crear múltiples entries, filtrar por tipo y verificar retorno.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getAllByPatient_retornaTodasLasEntries() {
        // TODO: Crear múltiples entries de diferentes tipos, verificar retorno completo.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
