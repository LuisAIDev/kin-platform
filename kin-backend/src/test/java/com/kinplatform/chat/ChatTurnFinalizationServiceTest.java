package com.kinplatform.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.common.event.ConversationCompletedEvent;
import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.ReportGeneratedEvent;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.infrastructure.outbox.TransactionalOutboxEventPublisher;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * ChatTurnFinalizationService — regresión de la frontera transaccional
 * (ADR-026): mensaje asistente + reporte + eventos outbox deben finalizarse
 * juntos dentro de un método {@code @Transactional} de un bean Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatTurnFinalizationService — finalización atómica del turno")
class ChatTurnFinalizationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ChatService chatService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private ChatTurnFinalizationService service;

    private ChatMessageResponse assistantResponse;

    @BeforeEach
    void setUp() {
        service = new ChatTurnFinalizationService(chatService, reportRepository, outboxEventPublisher);
        assistantResponse = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .projectId(PROJECT_ID)
                .role(MessageRole.ASSISTANT)
                .content("respuesta")
                .tokensUsed(7)
                .build();
    }

    @Test
    @DisplayName("finalizeTurn está anotado con @Transactional (requisito del outbox)")
    void finalizeTurn_deberiaEstarAnotadoConTransactional() throws Exception {
        var method = ChatTurnFinalizationService.class.getMethod(
                "finalizeTurn",
                UUID.class,
                UUID.class,
                String.class,
                ConversationDecision.class,
                ConsultingReport.class,
                List.class);
        assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    @DisplayName("Turno ASK: persiste assistant y publica eventos; NO persiste reporte")
    void finalizeTurn_turnoAsk_persisteAssistantYPublicaEventosSinReporte() {
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any(SaveMessageRequest.class)))
                .thenReturn(assistantResponse);
        List<DomainEvent> events = List.of(new ConversationCompletedEvent(PROJECT_ID, 1, 2, "ASK"));

        var result = service.finalizeTurn(
                USER_ID, PROJECT_ID, "respuesta", ConversationDecision.ask(null, 5, "pregunta"), null, events);

        assertSame(assistantResponse, result);

        var captor = ArgumentCaptor.forClass(SaveMessageRequest.class);
        verify(chatService).saveMessage(eq(USER_ID), eq(PROJECT_ID), captor.capture());
        assertEquals(MessageRole.ASSISTANT, captor.getValue().getRole());
        assertEquals("respuesta", captor.getValue().getContent());

        verify(reportRepository, never()).save(any(UUID.class), any(ConsultingReport.class));
        verify(outboxEventPublisher).publish(events.get(0));
    }

    @Test
    @DisplayName("Turno REPORT con informe: persiste assistant + reporte y publica eventos")
    void finalizeTurn_turnoReport_persisteAssistantYReporteYPublicaEventos() {
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any(SaveMessageRequest.class)))
                .thenReturn(assistantResponse);
        ConsultingReport report = ConsultingReport.empty();
        List<DomainEvent> events = List.of(
                new ReportGeneratedEvent(PROJECT_ID, "markdown"),
                new ConversationCompletedEvent(PROJECT_ID, 1, 10, "REPORT"));

        var result = service.finalizeTurn(
                USER_ID, PROJECT_ID, "informe", ConversationDecision.generateReport("informe"), report, events);

        assertSame(assistantResponse, result);
        verify(reportRepository).save(PROJECT_ID, report);
        verify(outboxEventPublisher).publish(events.get(0));
        verify(outboxEventPublisher).publish(events.get(1));
    }

    @Test
    @DisplayName("Sin eventos no se publica nada en el outbox")
    void finalizeTurn_sinEventos_noPublicaNada() {
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any(SaveMessageRequest.class)))
                .thenReturn(assistantResponse);

        service.finalizeTurn(USER_ID, PROJECT_ID, "respuesta", ConversationDecision.ask(null, 5, "q"), null, List.of());

        verify(outboxEventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Con el publicador REAL y SIN transacción activa, finalizeTurn lanza (guard del outbox)")
    void finalizeTurn_conPublicadorRealSinTransaccion_lanzaIllegalStateException() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TransactionalOutboxEventPublisher realPublisher = new TransactionalOutboxEventPublisher(jdbcTemplate, true);
        ChatTurnFinalizationService realService =
                new ChatTurnFinalizationService(chatService, reportRepository, realPublisher);

        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any(SaveMessageRequest.class)))
                .thenReturn(assistantResponse);
        List<DomainEvent> events = List.of(new ConversationCompletedEvent(PROJECT_ID, 1, 2, "ASK"));

        assertThatThrownBy(() -> realService.finalizeTurn(
                        USER_ID, PROJECT_ID, "respuesta", ConversationDecision.ask(null, 5, "q"), null, events))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transacción activa");
        // El guard del outbox impide escribir fuera de transacción.
        assertTrue(true);
    }
}



