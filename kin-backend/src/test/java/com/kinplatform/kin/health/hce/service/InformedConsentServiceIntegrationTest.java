package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateInformedConsentRequest;
import com.kinplatform.kin.health.hce.dto.InformedConsentResponse;
import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.kin.health.hce.repository.InformedConsentRepository;
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
class InformedConsentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InformedConsentRepository informedConsentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InformedConsentService informedConsentService;

    @Test
    void createConsent_happyPath_surgical_y_anesthesia_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear consentimiento SURGICAL y ANESTHESIA,
        // verificar persistencia con status=VALID y signedAt.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void createConsent_throwsWhenPatientNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (patientId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void revokeConsent_flujoCompleto_validoARevoked() {
        // TODO: Crear consentimiento VALID, revocar, verificar status=REVOKED, revokedAt, revocationReason.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void revokeConsent_throwsWhenAlreadyRevoked() {
        // TODO: Verificar que revocar consentimiento ya REVOKED lanza IllegalStateException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByPatient_retornaListaFiltrada() {
        // TODO: Crear múltiples consentimientos, verificar filtro por patientId.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}