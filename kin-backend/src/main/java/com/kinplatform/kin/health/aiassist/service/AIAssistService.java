package com.kinplatform.kin.health.aiassist.service;

import com.kinplatform.kin.health.aiassist.config.AIAssistProperties;
import com.kinplatform.kin.health.aiassist.domain.AIAssistRequest;
import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import com.kinplatform.kin.health.aiassist.port.AIAssistRepository;
import com.kinplatform.kin.health.aiassist.port.AIProviderPort;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.documents.port.ClinicalDocumentRepository;
import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.followup.domain.FollowUpPlan;
import com.kinplatform.kin.health.followup.port.FollowUpPlanRepository;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.physician.access.RelationshipNotActiveException;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AIAssistService {

    private static final Logger log = LoggerFactory.getLogger(AIAssistService.class);

    private final AIAssistRepository assistRepository;
    private final AIProviderPort aiProvider;
    private final AIAssistProperties properties;
    private final RelationshipAccessValidator accessValidator;
    private final AuditService auditService;
    private final TriageConsultationRepository triageRepository;
    private final FollowUpPlanRepository followUpRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalDocumentRepository documentRepository;

    public AIAssistService(
            AIAssistRepository assistRepository,
            AIProviderPort aiProvider,
            AIAssistProperties properties,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            TriageConsultationRepository triageRepository,
            FollowUpPlanRepository followUpRepository,
            AppointmentRepository appointmentRepository,
            ClinicalDocumentRepository documentRepository) {
        this.assistRepository = assistRepository;
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.accessValidator = accessValidator;
        this.auditService = auditService;
        this.triageRepository = triageRepository;
        this.followUpRepository = followUpRepository;
        this.appointmentRepository = appointmentRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public AIAssistRequest generateSummary(UUID physicianId, UUID patientId) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);

        List<TriageConsultation> triages = triageRepository.findByUserId(patientId);
        List<FollowUpPlan> plans = followUpRepository.findByPatientIdAndPhysicianId(patientId, physicianId);
        List<Appointment> appointments = appointmentRepository.findByPatientIdAndScheduledAtBetween(
                patientId, OffsetDateTime.now().minusDays(30), OffsetDateTime.now().plusDays(90));
        List<ClinicalDocument> documents = documentRepository.findActiveByPatientIdAndPhysicianId(patientId, physicianId);

        String inputData = buildSummaryData(triages, plans, appointments, documents);
        String prompt = buildSummaryPrompt(inputData);
        String response = aiProvider.generate(prompt);

        AIAssistRequest request = AIAssistRequest.of(AIAssistType.SUMMARY, inputData, response,
                physicianId, patientId, "resumen previo a consulta");
        assistRepository.save(request);
        auditService.logAccess(physicianId, AuditAction.VIEW_SUMMARY, AuditResourceType.DOCUMENTO,
                request.id(), patientId, Map.of("type", "AI_SUMMARY"));
        log.info("AIAssistService: resumen generado para paciente {} por medico {}", patientId, physicianId);
        return request;
    }

    @Transactional
    public AIAssistRequest organizeSymptoms(UUID patientId, UUID triageId) {
        requireEnabled();
        TriageConsultation consultation = triageRepository.findByIdAndUserId(triageId, patientId)
                .orElseThrow(() -> new IllegalArgumentException("Triaje no encontrado: " + triageId));

        String inputData = buildSymptomsData(consultation);
        String prompt = buildOrganizePrompt(inputData);
        String response = aiProvider.generate(prompt);

        AIAssistRequest request = AIAssistRequest.of(AIAssistType.ORGANIZE, inputData, response,
                patientId, patientId, "organizacion de sintomas");
        assistRepository.save(request);
        auditService.logAccess(patientId, AuditAction.VIEW_SUMMARY, AuditResourceType.DOCUMENTO,
                request.id(), patientId, Map.of("type", "AI_ORGANIZE"));
        return request;
    }

    @Transactional
    public AIAssistRequest prepareConsultation(UUID physicianId, UUID patientId) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);

        List<TriageConsultation> triages = triageRepository.findByUserId(patientId);
        String inputData = buildPrepareData(triages);
        String prompt = buildPreparePrompt(inputData);
        String response = aiProvider.generate(prompt);

        AIAssistRequest request = AIAssistRequest.of(AIAssistType.PREPARE, inputData, response,
                physicianId, patientId, "preparacion de consulta");
        assistRepository.save(request);
        auditService.logAccess(physicianId, AuditAction.VIEW_HISTORY, AuditResourceType.DOCUMENTO,
                request.id(), patientId, Map.of("type", "AI_PREPARE"));
        return request;
    }

    @Transactional
    public AIAssistRequest explainDifferential(UUID patientId, TriageResult differentialResult) {
        requireEnabled();
        String inputData = buildExplainData(differentialResult);
        String prompt = buildExplainPrompt(inputData);
        String response = aiProvider.generate(prompt);

        AIAssistRequest request = AIAssistRequest.of(AIAssistType.EXPLAIN, inputData, response,
                patientId, patientId, "explicacion diagnostico");
        assistRepository.save(request);
        auditService.logAccess(patientId, AuditAction.VIEW_SUMMARY, AuditResourceType.DOCUMENTO,
                request.id(), patientId, Map.of("type", "AI_EXPLAIN"));
        return request;
    }

    @Transactional
    public AIAssistRequest draftMessage(UUID physicianId, UUID patientId, String recommendation) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);

        String inputData = recommendation != null ? recommendation : "";
        String prompt = buildDraftPrompt(inputData);
        String response = aiProvider.generate(prompt);

        AIAssistRequest request = AIAssistRequest.of(AIAssistType.DRAFT, inputData, response,
                physicianId, patientId, "redaccion de mensaje");
        assistRepository.save(request);
        auditService.logAccess(physicianId, AuditAction.SEND_MESSAGE, AuditResourceType.MENSAJE,
                request.id(), patientId, Map.of("type", "AI_DRAFT"));
        return request;
    }

    @Transactional(readOnly = true)
    public List<AIAssistRequest> getHistory(UUID physicianId, UUID patientId) {
        requireEnabled();
        accessValidator.requireActiveRelationship(physicianId, patientId);
        return assistRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<AIAssistRequest> getMyHistory(UUID userId) {
        requireEnabled();
        return assistRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public AIAssistRequest getLastRequest(UUID patientId, AIAssistType type) {
        return assistRepository.findByPatientIdAndType(patientId, type).stream().findFirst().orElse(null);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new com.kinplatform.kin.health.aiassist.api.AIAssistDisabledException();
        }
    }

    private String buildSummaryData(List<TriageConsultation> triages, List<FollowUpPlan> plans,
                                     List<Appointment> appointments, List<ClinicalDocument> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Triajes realizados:\n");
        for (TriageConsultation t : triages) {
            sb.append("- ").append(t.createdAt()).append(": ");
            sb.append(t.results().stream().map(TriageConditionResult::name).collect(Collectors.joining(", ")));
            sb.append("\n");
        }
        sb.append("\n### Planes de seguimiento:\n");
        for (FollowUpPlan p : plans) {
            sb.append("- ").append(p.title()).append(" (").append(p.status()).append(")");
            if (p.isActive()) sb.append(" - ACTIVO");
            sb.append("\n");
        }
        sb.append("\n### Citas:\n");
        for (Appointment a : appointments) {
            sb.append("- ").append(a.scheduledAt().toString().substring(0, 16)).append(" [")
                    .append(a.status()).append("]\n");
        }
        sb.append("\n### Documentos compartidos:\n");
        for (ClinicalDocument d : documents) {
            sb.append("- ").append(d.fileName()).append(" (").append(d.status()).append(")\n");
        }
        return sb.toString();
    }

    private String buildSymptomsData(TriageConsultation consultation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sintomas reportados:\n");
        for (String s : consultation.symptoms()) {
            sb.append("- ").append(s).append("\n");
        }
        sb.append("\nCondiciones detectadas:\n");
        for (TriageConditionResult r : consultation.results()) {
            sb.append("- ").append(r.name()).append(" (probabilidad: ").append(r.probability()).append(")\n");
            sb.append("  Descripcion: ").append(r.description()).append("\n");
            sb.append("  Severidad: ").append(r.severity()).append(", Urgencia: ").append(r.urgency()).append("\n");
        }
        return sb.toString();
    }

    private String buildPrepareData(List<TriageConsultation> triages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Historial de triajes del paciente:\n");
        for (TriageConsultation t : triages) {
            sb.append("- ").append(t.createdAt()).append(": sintomas [")
                    .append(String.join(", ", t.symptoms())).append("]\n");
        }
        return sb.toString();
    }

    private String buildExplainData(TriageResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resultados del diagnostico diferencial:\n");
        sb.append("Confianza: ").append(result.confidence()).append("\n");
        for (TriageConditionResult r : result.results()) {
            sb.append("- ").append(r.name()).append(": ").append(r.probability()).append("\n");
            sb.append("  ").append(r.description()).append("\n");
        }
        return sb.toString();
    }

    private String buildSummaryPrompt(String data) {
        return """
            Eres un asistente medico de KIN. Resume el siguiente historial del paciente en 5 viñetas breves y claras para que el medico lo lea antes de una consulta.
            NO añadas informacion adicional, solo organiza lo que se te da:
            NO generes, modifiques ni interpretes datos clinicos. Solo presenta los datos que ya existen.

            %s
            """.formatted(data);
    }

    private String buildOrganizePrompt(String data) {
        return """
            Eres un asistente medico de KIN. Agrupa los siguientes sintomas por categorias (respiratorios, digestivos, musculares, etc.) de forma visual.
            NO añadas sintomas nuevos. Solo organiza los que se te dan.
            NO generes ni modifiques datos clinicos.

            %s
            """.formatted(data);
    }

    private String buildPreparePrompt(String data) {
        return """
            Eres un asistente medico de KIN. Basandote en el siguiente historial de triajes, genera un listado de preguntas para el medico sobre informacion faltante o dudosa.
            Las preguntas deben basarse exclusivamente en los datos proporcionados. NO inventes preguntas ni datos clinicos.
            NO sugerencias de tratamiento ni diagnostico. Solo preguntas.

            %s
            """.formatted(data);
    }

    private String buildExplainPrompt(String data) {
        return """
            Eres un asistente para pacientes de KIN. Explica de forma sencilla y tranquilizadora los siguientes hallazgos medicos.
            No uses tecnicismos y se empatico. El paciente debe entender el diagnostico.
            NO modifiques los datos numericos (probabilidades, scores). Solo explicalos.
            NO sugieras tratamientos, medicamentos ni diagnostico. Solo explica los resultados.
            Recuerda al paciente que debe consultar a su medico para cualquier decision.

            %s
            """.formatted(data);
    }

    private String buildDraftPrompt(String data) {
        return """
            Redacta un mensaje para el paciente basado en la siguiente recomendacion medica.
            Se claro y amable. NO añadas informacion que no este en la recomendacion.
            NO sugieras medicamentos ni tratamientos que no esten en la recomendacion.
            El medico puede editar o descartar el mensaje en cualquier momento.

            Recomendacion: %s
            """.formatted(data);
    }
}
