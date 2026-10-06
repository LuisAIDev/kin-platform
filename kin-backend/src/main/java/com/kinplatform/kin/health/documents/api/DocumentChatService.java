package com.kinplatform.kin.health.documents.api;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.common.context.Message;
import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.documents.config.DocumentProperties;
import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentChatMessage;
import com.kinplatform.kin.health.documents.domain.DocumentChatRole;
import com.kinplatform.kin.health.documents.port.DocumentChatRepository;
import com.kinplatform.kin.health.verification.WhoVerificationService;
import com.kinplatform.kin.health.verification.domain.ClinicalVerification;
import com.kinplatform.kin.health.verification.domain.VerificationStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conversación de IA sobre un documento clínico del paciente (ADR-041).
 *
 * <p>El chat está escopado a un documento concreto y solo es accesible por su
 * propietario (paciente), el médico asociado/que lo subió o un ADMIN (validado
 * en {@link DocumentService#requireAccessibleDocument}). Cada turno:</p>
 * <ol>
 *   <li>verifica contra la ICD-API/GHO de la OMS los hallazgos del documento
 *       antes de generar la respuesta ({@link WhoVerificationService});</li>
 *   <li>inyecta el texto extraído del documento y el resultado de la
 *       verificación como contexto del modelo;</li>
 *   <li>persiste usuario + asistente y audita el acceso.</li>
 * </ol>
 */
@Service
public class DocumentChatService {

    private static final Logger log = LoggerFactory.getLogger(DocumentChatService.class);

    /** Contrato de chat compartido (idéntico al chat de proyectos: 10.000 caracteres). */
    private static final int MAX_MESSAGE_LENGTH = 10_000;
    private static final int HISTORY_WINDOW = 30;
    private static final int MAX_CONTEXT_CHARS = 60_000;

    private static final String NO_TEXT_MESSAGE =
            "No pude leer el contenido de este documento de forma automática (puede ser un PDF escaneado "
                    + "o un formato sin capa de texto). Para una interpretación confiable, revisa el documento "
                    + "original o consulta con tu profesional de la salud.";

    private final DocumentChatRepository chatRepository;
    private final DocumentService documentService;
    private final WhoVerificationService whoVerificationService;
    private final AIResponder aiResponder;
    private final AuditService auditService;
    private final DocumentProperties properties;

    public DocumentChatService(
            DocumentChatRepository chatRepository,
            DocumentService documentService,
            WhoVerificationService whoVerificationService,
            AIResponder aiResponder,
            AuditService auditService,
            DocumentProperties properties) {
        this.chatRepository = chatRepository;
        this.documentService = documentService;
        this.whoVerificationService = whoVerificationService;
        this.aiResponder = aiResponder;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public ChatTurn sendMessage(UUID userId, UUID documentId, boolean admin, String content) {
        requireEnabled();
        String message = content == null ? "" : content.trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("El mensaje no puede superar los "
                    + MAX_MESSAGE_LENGTH + " caracteres");
        }

        ClinicalDocument document = documentService.requireAccessibleDocument(documentId, userId, admin);
        List<DocumentChatMessage> prior = chatRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
        String extractedText = documentService.ensureExtractedText(document);

        String assistantContent;
        String verificationStatus = "";
        if (extractedText == null || extractedText.isBlank()) {
            assistantContent = NO_TEXT_MESSAGE;
        } else {
            ClinicalVerification verification = whoVerificationService.verifyDocumentContext(extractedText, message);
            verificationStatus = verification.status().name();
            String systemPrompt = buildSystemPrompt(document, extractedText, verification);
            AIRequest request = new AIRequest(toHistory(prior), message, systemPrompt);
            assistantContent = aiResponder.respond(request);
        }

        DocumentChatMessage userMessage = chatRepository.save(DocumentChatMessage.of(
                documentId, userId, DocumentChatRole.USER, message));
        DocumentChatMessage assistantMessage = chatRepository.save(DocumentChatMessage.of(
                documentId, userId, DocumentChatRole.ASSISTANT, assistantContent));
        auditService.logAccess(userId, AuditAction.AI_ANALYZE_DOCUMENT, AuditResourceType.DOCUMENTO,
                documentId, document.patientId(), Map.of("fileName", document.fileName()));
        log.info("DocumentChatService: turno de análisis sobre documento {} por usuario {}", documentId, userId);
        return new ChatTurn(userMessage, assistantMessage, verificationStatus);
    }

    @Transactional(readOnly = true)
    public List<DocumentChatMessage> history(UUID userId, UUID documentId, boolean admin) {
        requireEnabled();
        documentService.requireAccessibleDocument(documentId, userId, admin);
        return chatRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    @Transactional
    public void clearConversation(UUID userId, UUID documentId, boolean admin) {
        requireEnabled();
        documentService.requireAccessibleDocument(documentId, userId, admin);
        chatRepository.deleteByDocumentId(documentId);
    }

    private String buildSystemPrompt(
            ClinicalDocument document, String extractedText, ClinicalVerification verification) {
        String text = truncate(extractedText, MAX_CONTEXT_CHARS);
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres el asistente de análisis de documentos de KIN Salud. ")
                .append("Ayudas a pacientes a entender SUS PROPIOS documentos médicos en lenguaje sencillo.\n")
                .append("Analisis procesado por KIN Medical con un motor de reglas fijas. ")
                .append("Apoyo informativo: NO sustituye la evaluacion ni el ")
                .append("diagnostico de un profesional de la salud.\n\n");

        prompt.append("## DOCUMENTO CLÍNICO\n");
        prompt.append("- Nombre: ").append(document.fileName()).append('\n');
        if (document.description() != null && !document.description().isBlank()) {
            prompt.append("- Descripción: ").append(document.description()).append('\n');
        }
        prompt.append('\n');

        prompt.append("## CONTENIDO EXTRAÍDO DEL DOCUMENTO (única fuente de valores)\n")
                .append(text).append("\n\n");

        prompt.append(whoVerificationService.promptSection(verification)).append('\n');

        prompt.append("## REGLAS OBLIGATORIAS\n")
                .append("1. Responde en español, con lenguaje sencillo y empático. NO uses markdown (sin #, **, |, tablas). ")
                .append("Usa TEXTO PLANO con secciones claras y saltos de línea.\n")
                .append("2. Usa ÚNICAMENTE los valores y datos presentes en el contenido extraído. ")
                .append("NUNCA inventes valores, unidades ni resultados.\n")
                .append("3. Si un dato no está en el documento, indícalo explícitamente.\n");
        if (verification != null && verification.status() == VerificationStatus.VERIFIED) {
            prompt.append("4. La verificación oficial contra la ICD-API de la OMS SÍ estuvo disponible: puedes ")
                    .append("referirte únicamente a los códigos listados en la sección de verificación. ")
                    .append("No menciones otros códigos CIE.\n");
        } else {
            prompt.append("4. NO inventes códigos CIE-10/CIE-11, prevalencias, estadísticas oficiales ")
                    .append("ni datos que no estén en el documento. Trabaja únicamente con la información ")
                    .append("del documento, las reglas médicas y los rangos de referencia verificables. ")
                    .append("NO incluyas notas del tipo 'no verificado contra fuentes oficiales' ni ")
                    .append("similares. El disclaimer oficial ya aparece en el encabezado del PDF.\n");
        }
        prompt.append("5. Recuerda al paciente que consulte con su profesional de la salud para interpretar ")
                .append("sus resultados.\n")
                .append("6. No compartas ni repitas contenido confidencial fuera del análisis que se te pide.\n");
        prompt.append("7. IMPORTANTE: NO uses markdown (sin #, **, |, tablas). Usa TEXTO PLANO con ");
        prompt.append("secciones separadas por líneas en blanco. Usa MAYÚSCULAS para títulos de sección, ");
        prompt.append("guiones (-) para listas, y líneas en blanco entre secciones.\n");
        prompt.append("8. IMPORTANTE: SOLO CARACTERES ASCII BÁSICOS + acentos españoles (áéíóúñÁÉÍÓÚÑ¿¡). ");
        prompt.append("PROHIBIDO: ≥ ≤ → → • ● ▪ × € € % º ª ® © § ¶ † ‡ • º ª. ");
        prompt.append("Usa solo: >= <= -> x . No uses ≥ ≤ → • ● ▪ × •. ");
        prompt.append("No uses emojis, no uses emojis, no símbolos matemáticos Unicode.\n");
        prompt.append("9. IMPORTANTE: Cuando termines el análisis, DETENTE. No agregues texto adicional. ");
        prompt.append("No repitas información. No inventes conclusiones.\n");
        return prompt.toString();
    }

    /** Historial acotado (ventana) convertido a {@link Message} para el proveedor. */
    private static List<Message> toHistory(List<DocumentChatMessage> prior) {
        int from = Math.max(0, prior.size() - HISTORY_WINDOW);
        return prior.subList(from, prior.size()).stream()
                .map(m -> m.role() == DocumentChatRole.USER
                        ? Message.user(m.content())
                        : Message.assistant(m.content()))
                .toList();
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new DocumentsDisabledException();
        }
    }

    /** Resultado de un turno: mensajes persistidos + estado de la verificación OMS. */
    public record ChatTurn(
            DocumentChatMessage userMessage,
            DocumentChatMessage assistantMessage,
            String verificationStatus) {}
}


