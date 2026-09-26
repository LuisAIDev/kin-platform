package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateReferralRequest;
import com.kinplatform.kin.health.hce.dto.ReferralResponse;
import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.repository.ReferralRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class ReferralServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferralService referralService;

    @Test
    void createReferral_happyPath_interconsultation_y_emergency_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Referral INTERCONSULTATION y EMERGENCY,
        // verificar persistencia con status=PENDING.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createReferral_throwsWhenPatientNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (patientId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void counterReferral_flujoCompleto_pendienteACompletado() {
        // TODO: Crear Referral PENDING, hacer counterReferral, verificar status=COMPLETED, counterreferralAt, counterreferralBy.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void counterReferral_throwsWhenCompleted() {
        // TODO: Verificar que counterReferral en COMPLETED lanza IllegalStateException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByPatient_retornaListaFiltrada() {
        // TODO: Crear múltiples referrals, verificar filtro por patientId.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByStatus_retornaListaFiltrada() {
        // TODO: Crear referrals con diferentes status, verificar filtro por status.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}