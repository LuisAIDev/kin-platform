package com.kinplatform.kin.health.documents.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.kin.health.documents.config.DocumentProperties;
import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentChatMessage;
import com.kinplatform.kin.health.documents.domain.DocumentChatRole;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.kin.health.documents.port.DocumentChatRepository;
import com.kinplatform.kin.health.verification.WhoVerificationService;
import com.kinplatform.kin.health.verification.domain.ClinicalVerification;
import com.kinplatform.kin.health.verification.domain.VerificationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests del servicio de conversación de IA sobre documentos clínicos (ADR-041):
 * persistencia de turnos, prompt con verificación OMS y degradado sin texto.
 */
@ExtendWith(MockitoExtension.class)
class DocumentChatServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @Mock
    private DocumentChatRepository chatRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private WhoVerificationService whoVerificationService;
    @Mock
    private AIResponder aiResponder;
    @Mock
    private AuditService auditService;

    private DocumentChatService service;

    @BeforeEach
    void setUp() {
        service = new DocumentChatService(
                chatRepository, documentService, whoVerificationService,
                aiResponder, auditService, new DocumentProperties());
        lenient().when(chatRepository.save(any(DocumentChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(whoVerificationService.promptSection(any()))
                .thenReturn("## VERIFICACIÓN CLÍNICA (fuente oficial OMS)\nEstado: VERIFIED\n");
    }

    private static ClinicalDocument document(String extractedText) {
        return ClinicalDocument.of(
                DOCUMENT_ID, "laboratorio.pdf", 10, "application/pdf", "key",
                USER, USER, null, "Examen", DocumentStatus.ACTIVE,
                OffsetDateTime.now(), OffsetDateTime.now(), extractedText);
    }

    @Test
    void sendMessage_conTexto_persisteTurnoYEnviaPromtConVerificacion() {
        when(documentService.requireAccessibleDocument(DOCUMENT_ID, USER, false)).thenReturn(document("Glucosa 110"));
        when(documentService.ensureExtractedText(any())).thenReturn("Glucosa en ayunas 110 mg/dL");
        when(whoVerificationService.verifyDocumentContext(any(), any()))
                .thenReturn(new ClinicalVerification(
                        VerificationStatus.VERIFIED,
                        List.of(new com.kinplatform.kin.health.verification.domain.DiagnosticMatch(
                                "5B75", "Diabetes", "glucosa", "2024-01")),
                        List.of(),
                        "verificado"));
        var captor = org.mockito.ArgumentCaptor.forClass(AIRequest.class);
        when(aiResponder.respond(captor.capture())).thenReturn("Tu glucosa está en 110 mg/dL...");

        DocumentChatService.ChatTurn turn = service.sendMessage(USER, DOCUMENT_ID, false, "¿qué significa?");

        AIRequest request = captor.getValue();
        assertTrue(request.systemPrompt().contains("laboratorio.pdf"));
        assertTrue(request.systemPrompt().contains("VERIFICACIÓN CLÍNICA"));
        assertEquals("¿qué significa?", request.userMessage());
        assertEquals(DocumentChatRole.USER, turn.userMessage().role());
        assertEquals(DocumentChatRole.ASSISTANT, turn.assistantMessage().role());
        assertTrue(turn.assistantMessage().content().contains("110 mg/dL"));
        verify(chatRepository, times(2)).save(any(DocumentChatMessage.class));
        verify(auditService).logAccess(eq(USER), eq(AuditAction.AI_ANALYZE_DOCUMENT), any(), eq(DOCUMENT_ID),
                eq(USER), any());
    }

    @Test
    void sendMessage_sinTextoNoLlamaAI_yDevuelveAviso() {
        when(documentService.requireAccessibleDocument(DOCUMENT_ID, USER, false)).thenReturn(document(""));
        when(documentService.ensureExtractedText(any())).thenReturn("");

        DocumentChatService.ChatTurn turn = service.sendMessage(USER, DOCUMENT_ID, false, "¿qué significa?");

        verify(aiResponder, never()).respond(any(AIRequest.class));
        assertTrue(turn.assistantMessage().content().contains("escaneado"));
    }

    @Test
    void sendMessage_mensajeVacio_lanza() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendMessage(USER, DOCUMENT_ID, false, "   "));
    }

    @Test
    void sendMessage_usuarioAjeno_propagaDenegacion() {
        DocumentAccessDeniedException denied = new DocumentAccessDeniedException("denegado");
        when(documentService.requireAccessibleDocument(DOCUMENT_ID, USER, false)).thenThrow(denied);

        assertThrows(DocumentAccessDeniedException.class,
                () -> service.sendMessage(USER, DOCUMENT_ID, false, "hola"));
    }

    @Test
    void history_devuelveSoloMensajesDelDocumento() {
        when(documentService.requireAccessibleDocument(DOCUMENT_ID, USER, false))
                .thenReturn(document("texto"));
        when(chatRepository.findByDocumentIdOrderByCreatedAtAsc(DOCUMENT_ID))
                .thenReturn(List.of(DocumentChatMessage.of(
                        DOCUMENT_ID, USER, DocumentChatRole.USER, "hola")));

        List<DocumentChatMessage> history = service.history(USER, DOCUMENT_ID, false);

        assertEquals(1, history.size());
    }
}

