package com.kinplatform.kin.health.aiassist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.aiassist.InMemoryAIAssistRepository;
import com.kinplatform.kin.health.aiassist.config.AIAssistProperties;
import com.kinplatform.kin.health.aiassist.domain.AIAssistRequest;
import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import com.kinplatform.kin.health.aiassist.port.AIProviderPort;
import com.kinplatform.kin.health.documents.InMemoryClinicalDocumentRepository;
import com.kinplatform.kin.health.followup.InMemoryFollowUpRepositories;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.physician.access.RelationshipNotActiveException;
import com.kinplatform.kin.health.telemedicine.InMemoryTelemedicineRepositories;
import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de seguridad del historial de AI Assist (P1-1): solo el médico con
 * relaci\u00f3n ACTIVE puede consultar el historial cl\u00ednico de un paciente.
 */
class AIAssistServiceTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID OTHER_PATIENT = UUID.randomUUID();

    private InMemoryAIAssistRepository assistRepository;
    private AIAssistService service;
    private AIAssistProperties properties;

    @BeforeEach
    void setUp() {
        assistRepository = new InMemoryAIAssistRepository();
        properties = new AIAssistProperties();
        properties.setEnabled(true);

        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));

        service = new AIAssistService(
                assistRepository,
                stubAIProvider,
                properties,
                new RelationshipAccessValidator(physicians.patientRepository()),
                new com.kinplatform.common.audit.api.AuditService(null, null, null,
                        disabledAuditProps()),
                new InMemoryTriageConsultationRepository(),
                new InMemoryFollowUpRepositories().planRepository(),
                new InMemoryTelemedicineRepositories().appointmentRepository(),
                new InMemoryClinicalDocumentRepository());
    }

    private com.kinplatform.common.audit.config.AuditProperties disabledAuditProps() {
        var auditProps = new com.kinplatform.common.audit.config.AuditProperties();
        auditProps.setEnabled(false);
        return auditProps;
    }

    private void seedHistory(UUID patientId) {
        assistRepository.save(AIAssistRequest.of(
                AIAssistType.SUMMARY, "datos clinicos de " + patientId, "resumen",
                PHYSICIAN, patientId, "test"));
    }

    @Test
    void getHistory_medicoConRelacionActiva_deberiaDevolverHistorialDelPaciente() {
        seedHistory(PATIENT);

        List<AIAssistRequest> history = service.getHistory(PHYSICIAN, PATIENT);

        assertEquals(1, history.size());
        assertEquals(PATIENT, history.get(0).patientId());
        assertTrue(history.get(0).inputData().contains("datos clinicos de " + PATIENT));
    }

    @Test
    void getHistory_medicoSinRelacionActiva_deberiaLanzar() {
        seedHistory(OTHER_PATIENT);

        assertThrows(RelationshipNotActiveException.class,
                () -> service.getHistory(PHYSICIAN, OTHER_PATIENT));
    }

    @Test
    void getHistory_pacienteNoAutorizado_deberiaLanzar() {
        seedHistory(OTHER_PATIENT);

        assertThrows(RelationshipNotActiveException.class,
                () -> service.getHistory(PATIENT, OTHER_PATIENT));
    }

    @Test
    void getHistory_medicoConRelacionActiva_noDebeDevolverHistorialDeOtroPaciente() {
        seedHistory(PATIENT);
        seedHistory(OTHER_PATIENT);

        List<AIAssistRequest> history = service.getHistory(PHYSICIAN, PATIENT);

        assertEquals(1, history.size());
        assertEquals(PATIENT, history.get(0).patientId());
        assertTrue(history.stream().noneMatch(r -> r.patientId().equals(OTHER_PATIENT)));
    }

    @Test
    void getMyHistory_paciente_deberiaDevolverSoloSusRegistros() {
        assistRepository.save(AIAssistRequest.of(
                AIAssistType.EXPLAIN, "datos del paciente A", "explicacion",
                PATIENT, PATIENT, "test"));
        assistRepository.save(AIAssistRequest.of(
                AIAssistType.EXPLAIN, "datos del paciente B", "explicacion",
                OTHER_PATIENT, OTHER_PATIENT, "test"));

        List<AIAssistRequest> myHistory = service.getMyHistory(PATIENT);

        assertEquals(1, myHistory.size());
        assertEquals(PATIENT, myHistory.get(0).userId());
        assertTrue(myHistory.stream().noneMatch(r -> r.userId().equals(OTHER_PATIENT)));
    }

    @Test
    void getMyHistory_pacienteSinRegistros_deberiaDevolverVacio() {
        List<AIAssistRequest> myHistory = service.getMyHistory(PATIENT);

        assertTrue(myHistory.isEmpty());
    }

    private static final AIProviderPort stubAIProvider = new AIProviderPort() {
        @Override
        public String generate(String prompt) {
            return "respuesta IA de prueba";
        }

        @Override
        public String generate(String prompt, int maxTokens, double temperature) {
            return "respuesta IA de prueba";
        }
    };
}

