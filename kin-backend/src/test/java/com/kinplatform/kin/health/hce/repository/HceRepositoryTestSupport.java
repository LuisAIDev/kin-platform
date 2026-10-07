package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Base para los tests de repositorio HCE. Carga el contexto completo con
 * PostgreSQL real (Testcontainers) y Flyway V1..V75, y siembra el árbol de
 * dependencias mínimo (2 usuarios + 1 encounter) para satisfacer las FK reales.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class HceRepositoryTestSupport extends PostgresTestSupport {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected EncounterRepository encounterRepository;

    protected UUID patientId;
    protected UUID physicianId;
    protected UUID organizationId;
    protected UUID encounterId;

    @BeforeEach
    void seedDependencies() {
        User patient = userRepository.saveAndFlush(User.builder()
                .email("hce-patient-" + UUID.randomUUID() + "@kin.test")
                .passwordHash("x")
                .fullName("HCE Patient")
                .role(UserRole.PATIENT)
                .build());

        User physician = userRepository.saveAndFlush(User.builder()
                .email("hce-physician-" + UUID.randomUUID() + "@kin.test")
                .passwordHash("x")
                .fullName("HCE Physician")
                .role(UserRole.PHYSICIAN)
                .build());

        patientId = patient.getId();
        physicianId = physician.getId();
        organizationId = UUID.randomUUID();

        Encounter encounter = encounterRepository.saveAndFlush(Encounter.builder()
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .build());
        encounterId = encounter.getId();
    }

    protected Encounter newEncounter() {
        return encounterRepository.saveAndFlush(Encounter.builder()
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .status(Encounter.EncounterStatus.IN_PROGRESS)
                .build());
    }
}
