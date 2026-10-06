package com.kinplatform.chat;

import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finalización transaccional del turno de chat (ADR-026).
 *
 * <p>Persiste en UNA única transacción Spring corta las tres operaciones que
 * conceptualmente cierran el turno y deben ser atómicas:</p>
 *
 * <ol>
 *   <li>mensaje del asistente ({@link ChatMessage}),</li>
 *   <li>{@link ConsultingReport} (si el turno completó {@code REPORT}),</li>
 *   <li>eventos de dominio producidos por el pipeline, publicados en el outbox
 *       transaccional ({@link OutboxEventPublisher}).</li>
 * </ol>
 *
 * <p>Si cualquiera de las tres falla se produce {@code ROLLBACK}: no queda un
 * mensaje persistido sin sus eventos ni un reporte huérfano. La llamada a la IA
 * NUNCA ocurre dentro de esta transacción: el pipeline se ejecuta antes, en el
 * hilo de streaming, y aquí solo se persiste el resultado ya generado.</p>
 */
@Service
public class ChatTurnFinalizationService {

    private final ChatService chatService;
    private final ReportRepository reportRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Autowired
    public ChatTurnFinalizationService(
            ChatService chatService, ReportRepository reportRepository, OutboxEventPublisher outboxEventPublisher) {
        this.chatService = chatService;
        this.reportRepository = reportRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    /**
     * Finaliza el turno dentro de una transacción Spring.
     *
     * @param userId            dueño del proyecto (validación de ownership)
     * @param projectId         proyecto de la conversación
     * @param assistantContent  contenido final del mensaje del asistente
     * @param decision          decisión del turno (puede ser {@code null})
     * @param consultingReport  informe de consultoría generado ({@code null}
     *                          salvo en turnos {@code REPORT})
     * @param events            eventos de dominio producidos por el pipeline
     *                          (puede ser {@code null} o vacío)
     * @return el mensaje del asistente persistido
     */
    @Transactional
    public ChatMessageResponse finalizeTurn(
            UUID userId,
            UUID projectId,
            String assistantContent,
            ConversationDecision decision,
            ConsultingReport consultingReport,
            List<DomainEvent> events) {
        SaveMessageRequest assistantRequest = new SaveMessageRequest();
        assistantRequest.setRole(MessageRole.ASSISTANT);
        assistantRequest.setContent(assistantContent);
        ChatMessageResponse assistant = chatService.saveMessage(userId, projectId, assistantRequest);

        if (decision != null && decision.action() == ConversationDecision.Action.REPORT && consultingReport != null) {
            reportRepository.save(projectId, consultingReport);
        }

        if (events != null) {
            for (DomainEvent event : events) {
                outboxEventPublisher.publish(event);
            }
        }

        return assistant;
    }
}



