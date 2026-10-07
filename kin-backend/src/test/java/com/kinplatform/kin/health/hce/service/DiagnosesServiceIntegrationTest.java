package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.dto.DiagnosesResponse;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class DiagnosesServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DiagnosesRepository diagnosesRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiagnosesService diagnosesService;

    @Test
    void addDiagnosis_happyPath_secundario_y_principal_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter,
        // agregar diagnóstico SECUNDARIO, luego agregar PRINCIPAL (debe fallar si ya existe),
        // verificar persistencia con diagnosis_type correcto.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addDiagnosis_throwsWhenEncounterNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (encounterId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void addDiagnosis_throwsWhenPrincipalAlreadyExists() {
        // TODO: Verificar que agregar segundo PRINCIPAL lanza IllegalStateException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void setPrincipal_happyPath_desmarcaAnteriorMarcaNuevo() {
        // TODO: Crear dos diagnósticos, setPrincipal en el segundo,
        // verificar que el primero pasa a SECUNDARIO y el segundo a PRINCIPAL.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getPrincipal_retornaEmptyCuandoNoHayPrincipal() {
        // TODO: Verificar getPrincipal retorna Optional.empty() si no hay PRINCIPAL activo.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
