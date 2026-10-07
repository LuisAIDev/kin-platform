package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreateClinicalAttachmentRequest;
import com.kinplatform.kin.health.hce.dto.ClinicalAttachmentResponse;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.repository.ClinicalAttachmentRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Disabled("Docker required - TD-INTEGRATION-REPO-HCE. Ejecutar cuando el runner CI tenga Docker daemon.")
@Testcontainers
@Transactional
class ClinicalAttachmentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ClinicalAttachmentRepository clinicalAttachmentRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicalAttachmentService clinicalAttachmentService;

    @Test
    void uploadAttachment_happyPath_labResult_y_imaging_verificarEnBD() {
        // TODO: Crear User paciente + User médico, crear Encounter,
        // subir attachment LAB_RESULT con loincCode y IMAGING con dicomStudyUid,
        // verificar persistencia con FK a encounter_id y patient_id.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void uploadAttachment_throwsWhenEncounterNotFound() {
        // TODO: Verificar que FK violation bloquea la inserción (encounterId inexistente).
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void uploadAttachment_throwsWhenAttachmentTypeInvalid() {
        // TODO: Verificar que attachmentType null lanza excepción.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void uploadAttachment_throwsWhenPerformedAtFuture() {
        // TODO: Verificar que performedAt futuro lanza IllegalArgumentException.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByEncounter_retornaListaFiltrada() {
        // TODO: Crear múltiples attachments, verificar filtro por encounterId.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }

    @Test
    void getByPatientAndType_retornaFiltradoPorTipo() {
        // TODO: Crear attachments de diferentes tipos, verificar filtro por patientId + attachmentType.
        // ACTIVAR cuando Docker esté disponible en el runner.
    }
}
