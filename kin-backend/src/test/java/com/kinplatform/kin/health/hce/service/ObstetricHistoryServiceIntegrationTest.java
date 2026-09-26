package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateObstetricHistoryRequest;
import com.kinplatform.kin.health.hce.dto.ObstetricHistoryResponse;
import com.kinplatform.kin.health.hce.entity.ObstetricHistory;
import com.kinplatform.kin.health.hce.repository.ObstetricHistoryRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class ObstetricHistoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ObstetricHistoryRepository obstetricHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObstetricHistoryService obstetricHistoryService;

    @Test
    void upsertHistory_creaYActualiza_verificarEnBD() {
        // TODO: Crear User paciente, upsert historial obstétrico (crear),
        // luego actualizar con upsert, verificar persistencia con gravida/para/abortions.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void upsertHistory_validaReglaGravidaMayorOSuma() {
        // TODO: Verificar que gravida < suma(para+abortions+ectopic+stillbirths) lanza excepción.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void upsertHistory_conCurrentPregnancyTrue_verificarEnBD() {
        // TODO: Crear historial con currentPregnancy=true, verificar persistencia.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByPatientId_retornaRegistroFiltrado() {
        // TODO: Crear múltiples historiales, verificar filtro por patientId.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}