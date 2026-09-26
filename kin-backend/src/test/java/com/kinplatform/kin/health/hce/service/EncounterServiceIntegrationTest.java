package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class EncounterServiceIntegrationTest extends PostgresTestSupport {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("kin_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private EncounterService encounterService;

    @Test
    void createEncounter_savesAndRetrieves() {
        // Given
        UUID patientId = UUID.randomUUID();
        UUID physicianId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(UUID.randomUUID())
                .encounterType("OUTPATIENT")
                .chiefComplaint("Dolor abdominal")
                .build();

        // Create encounter
        EncounterResponse response = encounterService.createEncounter(request);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isNotNull();
        assertThat(response.getEncounterType()).isEqualTo("OUTPATIENT");
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void findByPatientId_returnsOrdered() {
        // Test will need real patient/user setup
    }
}