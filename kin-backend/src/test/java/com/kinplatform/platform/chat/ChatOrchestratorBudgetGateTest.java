package com.kinplatform.platform.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.ai.guardrails.PromptGuardrail;
import com.kinplatform.common.ai.usage.AiBudgetControlService;
import com.kinplatform.common.ai.usage.ReservationContext;
import com.kinplatform.platform.chat.dto.ChatMessageResponse;
import com.kinplatform.platform.chat.dto.ChatRequest;
import com.kinplatform.platform.chat.dto.ChatResponse;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.conversation.ConversationOrchestrator;
import com.kinplatform.common.conversation.ConversationTurn;
import com.kinplatform.common.conversation.ResponseValidation;
import com.kinplatform.common.conversation.TurnDirective;
import com.kinplatform.common.conversation.TurnResult;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.platform.usage.AiBudgetExceededException;
import com.kinplatform.platform.usage.AiReservation;
import com.kinplatform.platform.usage.UsagePeriod;
import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.service.SubscriptionValidatorService;
import com.kinplatform.platform.project.Project;
import com.kinplatform.platform.project.ProjectRepository;
import com.kinplatform.platform.project.ProjectStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Verifica el gate de presupuesto de IA en el orquestador de chat: se reserva
 * el presupuesto ANTES de orquestar (y por tanto antes de llamar a DeepSeek);
 * si el presupuesto es insuficiente, bloquea (403) o emite un mensaje amigable
 * por SSE sin llamar al pipeline.
 */
@ExtendWith(MockitoExtension.class)
class ChatOrchestratorBudgetGateTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ChatService chatService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ConversationOrchestrator conversationOrchestrator;

    @Mock
    private SubscriptionValidatorService subscriptionValidator;

    @Mock
    private AiBudgetControlService budgetControlService;

    private ReservationContext reservationContext;
    private ChatOrchestratorServiceImpl orchestrator;
    private ChatRequest request;
    private Project project;

    @BeforeEach
    void setUp() {
        reservationContext = new ReservationContext();
        reservationContext.clear();
        orchestrator = new ChatOrchestratorServiceImpl(
                chatService,
                projectRepository,
                new ObjectMapper(),
                conversationOrchestrator,
                new PromptGuardrail(),
                reportRepositoryNoOp(),
                subscriptionValidator,
                budgetControlService,
                reservationContext,
                null);

        request = new ChatRequest();
        request.setContent("hola");

        var user = User.builder()
                .id(USER_ID)
                .email("u@t.com")
                .fullName("U")
                .role(UserRole.FREE)
                .build();
        project = Project.builder()
                .id(PROJECT_ID)
                .user(user)
                .title("Proyecto")
                .status(ProjectStatus.DRAFT)
                .build();
    }

    private void stubCommon() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(java.util.Optional.of(project));
        when(chatService.saveMessage(eq(USER_ID), eq(PROJECT_ID), any()))
                .thenReturn(message(MessageRole.USER))
                .thenReturn(message(MessageRole.ASSISTANT));
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID)).thenReturn(List.of());
    }

    private ChatMessageResponse message(MessageRole role) {
        return ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .projectId(PROJECT_ID)
                .role(role)
                .content("ok")
                .tokensUsed(0)
                .build();
    }

    private PricingPlan plan() {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .code("FREE")
                .name("GRATIS")
                .price(BigDecimal.ZERO)
                .maxProjects(3)
                .aiBudgetUsd(new BigDecimal("0.50"))
                .build();
    }

    private ReportRepository reportRepositoryNoOp() {
        return new ReportRepository() {
            @Override
            public int save(UUID projectId, com.kinplatform.platform.reporting.report.model.ConsultingReport report) {
                return 0;
            }

            @Override
            public java.util.Optional<StoredReport> findLatest(UUID projectId) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<StoredReport> findByVersion(UUID projectId, int version) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<ReportVersionInfo> listVersions(UUID projectId) {
                return java.util.List.of();
            }
        };
    }

    @Test
    void processMessage_presupuestoAgotado_lanza403SinOrquestar() {
        stubCommon();
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(budgetControlService.reserve(eq(USER_ID), any(), eq("hola"), any()))
                .thenThrow(new AiBudgetExceededException(AiBudgetControlService.BUDGET_EXCEEDED_MESSAGE));

        assertThrows(AiBudgetExceededException.class, () -> orchestrator.processMessage(USER_ID, PROJECT_ID, request));
        verify(conversationOrchestrator, never()).orchestrate(any(ConversationTurn.class));
        assertTrue(reservationContext.current() == null);
    }

    @Test
    void processMessage_presupuestoDisponible_procede() {
        stubCommon();
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        var reservation = new AiReservation(USER_ID, UsagePeriod.current(), new BigDecimal("0.01"));
        when(budgetControlService.reserve(eq(USER_ID), any(), eq("hola"), any()))
                .thenReturn(reservation);
        when(conversationOrchestrator.orchestrate(any(ConversationTurn.class))).thenReturn(turnResultAsk());

        ChatResponse response = orchestrator.processMessage(USER_ID, PROJECT_ID, request);

        assertEquals("¿pregunta?", response.getContent());
        verify(conversationOrchestrator).orchestrate(any(ConversationTurn.class));
        assertTrue(reservationContext.current() == null);
    }

    @Test
    void processMessageStream_presupuestoAgotado_emiteMensajeSinPipeline() {
        stubCommon();
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(budgetControlService.reserve(eq(USER_ID), any(), eq("hola"), any()))
                .thenThrow(new AiBudgetExceededException(AiBudgetControlService.BUDGET_EXCEEDED_MESSAGE));

        SseEmitter emitter = orchestrator.processMessageStream(USER_ID, PROJECT_ID, request);

        assertNotNull(emitter);
        verify(conversationOrchestrator, never()).orchestrateStreamWithOutcome(any(ConversationTurn.class));
    }

    private TurnResult turnResultAsk() {
        var ctx = ProjectContext.fromProject("Proyecto", "Desc", "Software");
        var decision = ConversationDecision.ask(AnalyzedDimension.SECTOR, 5, "pregunta");
        var directive = new TurnDirective(
                com.kinplatform.common.conversation.ConversationPhase.EXPLORATION,
                ConversationDecision.Action.ASK,
                AnalyzedDimension.SECTOR,
                com.kinplatform.common.conversation.CommunicationMode.QUESTION,
                com.kinplatform.common.conversation.TurnConstraints.question());
        return new TurnResult(ctx, decision, directive, "¿pregunta?", ResponseValidation.ok(), null, List.of());
    }
}









