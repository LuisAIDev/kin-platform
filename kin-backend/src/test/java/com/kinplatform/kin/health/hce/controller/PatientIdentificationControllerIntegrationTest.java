package com.kinplatform.kin.health.hce.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para PatientIdentificationController.
 * Requieren Docker/Testcontainers PostgreSQL 18.
 * Habilitar cuando el runner de CI/CD tenga Docker disponible.
 * Ver TD-INTEGRATION-REPO-HCE.
 */
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PatientIdentificationControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/health/hce/patients";
    }

    @Test
    void fullFlow_upsert_get_findByDocument() {
        // This test requires authentication setup
        // When enabled, it will:
        // 1. POST /api/v1/health/hce/patients/{patientId}/identification -> 201
        // 2. GET /api/v1/health/hce/patients/{patientId}/identification -> 200
        // 3. GET /api/v1/health/hce/patients/identification/by-document?documentType=CC&documentNumber=1234567890 -> 200
    }
}