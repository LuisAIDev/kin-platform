package com.kinplatform.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.guardrails.PromptGuardrail;
import com.kinplatform.ai.usage.AiBudgetControlService;
import com.kinplatform.ai.usage.ReservationContext;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.common.context.Message;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.StreamingTurnOutcome;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.kin.export.intent.ExportAction;
import com.kinplatform.kin.export.intent.ExportChatIntentService;
import com.kinplatform.platform.reporting.report.ReportRepository;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.usage.AiBudgetExceededException;
import com.kinplatform.kin.usage.AiReservation;
import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.project.ProjectRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Orquestador de chat. Tras la consolidación del runtime (Fase 5.2.1) y del
 * Conversation Orchestrator (Fase 5.6, ADR-013) NO contiene lógica de negocio
 * ni flujos ad-hoc: ambos endpoints ({@code /chat} y {@code /chat/stream})
 * delegan en {@link ConversationOrchestrator}, que resuelve la directiva en
 * Java y delega la ejecución en el pipeline. Este servicio solo se ocupa de la
 * I/O HTTP: persistir los mensajes y emitir el SSE.
 */
@Service
public class ChatOrchestratorServiceImpl implements ChatOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestratorServiceImpl.class);
    private static final long SSE_TIMEOUT = 180_000L;
    private static final String BLOCKED_MESSAGE =
            "No puedo procesar esa solicitud: parece contener instrucciones que intentan "
                    + "manipular el comportamiento del asistente. Por favor, reformúlala.";

    private final ChatService chatService;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;
    private final ConversationOrchestrator conversationOrchestrator;
    private final PromptGuardrail promptGuardrail;
    private final ReportRepository reportRepository;
    private final SubscriptionValidatorService subscriptionValidator;
    private final AiBudgetControlService budgetControlService;
    private final ReservationContext reservationContext;
    private final ExportChatIntentService exportChatIntentService;
    private final ChatTurnFinalizationService finalizationService;
    private final ExecutorService pipelineExecutor;
    private final boolean useSynchronousExecutor;

    /** Ejecutor por defecto: pool de un solo hilo para el pipeline async. */
    private static ExecutorService defaultPipelineExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kin-pipeline-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    /** Sin repo de reportes: el runtime conserva el comportamiento previo. */
    private static final ReportRepository NO_OP_REPORT_REPOSITORY = new ReportRepository() {
        @Override
        public int save(UUID projectId, ConsultingReport report) {
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
        public List<ReportVersionInfo> listVersions(UUID projectId) {
            return List.of();
        }
    };

    /**
     * Constructor de producción: inyecta el {@link ChatTurnFinalizationService}
     * que persiste mensaje asistente + reporte + eventos outbox en una
     * transacción corta tras completarse el turno (ADR-026).
     */
    @Autowired
    public ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator,
            PromptGuardrail promptGuardrail,
            ReportRepository reportRepository,
            SubscriptionValidatorService subscriptionValidator,
            AiBudgetControlService budgetControlService,
            ReservationContext reservationContext,
            ExportChatIntentService exportChatIntentService,
            ChatTurnFinalizationService finalizationService) {
        this(
                chatService,
                projectRepository,
                objectMapper,
                conversationOrchestrator,
                promptGuardrail,
                reportRepository,
                subscriptionValidator,
                budgetControlService,
                reservationContext,
                exportChatIntentService,
                finalizationService,
                defaultPipelineExecutor(),
                false);
    }

    /**
     * Constructor de compatibilidad (10 args) sin servicio de finalización:
     * conserva el comportamiento previo (guardar mensaje asistente + reporte,
     * sin publicación de eventos). Lo usan tests que construyen el servicio
     * directamente.
     */
    public ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator,
            PromptGuardrail promptGuardrail,
            ReportRepository reportRepository,
            SubscriptionValidatorService subscriptionValidator,
            AiBudgetControlService budgetControlService,
            ReservationContext reservationContext,
            ExportChatIntentService exportChatIntentService) {
        this(
                chatService,
                projectRepository,
                objectMapper,
                conversationOrchestrator,
                promptGuardrail,
                reportRepository,
                subscriptionValidator,
                budgetControlService,
                reservationContext,
                exportChatIntentService,
                null,
                defaultPipelineExecutor(),
                false);
    }

    /**
     * Constructor completo para tests: permite inyectar un executor síncrono.
     * No usa servicio de finalización (comportamiento legacy).
     */
    ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator,
            PromptGuardrail promptGuardrail,
            ReportRepository reportRepository,
            SubscriptionValidatorService subscriptionValidator,
            AiBudgetControlService budgetControlService,
            ReservationContext reservationContext,
            ExportChatIntentService exportChatIntentService,
            ExecutorService pipelineExecutor,
            boolean useSynchronousExecutor) {
        this(
                chatService,
                projectRepository,
                objectMapper,
                conversationOrchestrator,
                promptGuardrail,
                reportRepository,
                subscriptionValidator,
                budgetControlService,
                reservationContext,
                exportChatIntentService,
                null,
                pipelineExecutor,
                useSynchronousExecutor);
    }

    /**
     * Constructor completo para tests de cadena real: permite inyectar el
     * {@link ChatTurnFinalizationService} y un executor síncrono.
     */
    ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator,
            PromptGuardrail promptGuardrail,
            ReportRepository reportRepository,
            SubscriptionValidatorService subscriptionValidator,
            AiBudgetControlService budgetControlService,
            ReservationContext reservationContext,
            ExportChatIntentService exportChatIntentService,
            ChatTurnFinalizationService finalizationService,
            ExecutorService pipelineExecutor,
            boolean useSynchronousExecutor) {
        this.chatService = chatService;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
        this.conversationOrchestrator = conversationOrchestrator;
        this.promptGuardrail = promptGuardrail;
        this.reportRepository = reportRepository == null ? NO_OP_REPORT_REPOSITORY : reportRepository;
        this.subscriptionValidator = subscriptionValidator;
        this.budgetControlService = budgetControlService;
        this.reservationContext = reservationContext;
        this.exportChatIntentService = exportChatIntentService;
        this.finalizationService = finalizationService;
        this.pipelineExecutor = pipelineExecutor;
        this.useSynchronousExecutor = useSynchronousExecutor;
    }

    /**
     * Constructor de compatibilidad (6 args): usado antes de la capa de
     * presupuesto de IA (Fase 1). Sin gate de presupuesto y sin contexto de
     * reserva (el proveedor no atribuye uso).
     */
    public ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator,
            PromptGuardrail promptGuardrail,
            ReportRepository reportRepository) {
        this(
                chatService,
                projectRepository,
                objectMapper,
                conversationOrchestrator,
                promptGuardrail,
                reportRepository,
                null,
                null,
                null,
                null);
    }

    /**
     * Constructor de compatibilidad (4 args): usaba la firma previa antes de la
     * capa de guardrails (Fase 15) y de la persistencia del reporte. Activa los
     * guardrails por defecto y no persiste reportes.
     */
    public ChatOrchestratorServiceImpl(
            ChatService chatService,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper,
            ConversationOrchestrator conversationOrchestrator) {
        this(
                chatService,
                projectRepository,
                objectMapper,
                conversationOrchestrator,
                new PromptGuardrail(),
                NO_OP_REPORT_REPOSITORY,
                null,
                null,
                null,
                null);
    }

    /**
     * Sin {@code @Transactional}: la llamada a la IA (orchestrate) es I/O externa
     * lenta y no debe retener una conexi�n JDBC durante segundos. La persistencia
     * de mensajes se realiza a trav�s de {@link ChatService}, que s� aplica sus
     * propias transacciones de corta vida por operaci�n (saveMessage,
     * getConversationHistory).
     */
    @Override
    public ChatResponse processMessage(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        if (isBlocked(request.getContent())) {
            var userMessage = saveUserMessage(userId, projectId, request.getContent());
            var assistantMessage = saveAssistantMessage(userId, projectId, BLOCKED_MESSAGE);
            return ChatResponse.builder()
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .content(BLOCKED_MESSAGE)
                    .tokensUsed(0)
                    .build();
        }
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId);
        var reservation = reserveBudget(userId, request, history);
        setReservation(reservation);
        var turn = new ConversationTurn(
                projectId,
                userId,
                request.getContent(),
                history,
                project.getTitle(),
                project.getDescription(),
                project.getCategory() != null ? project.getCategory().getCode() : null);
        try {
            var result = conversationOrchestrator.orchestrate(turn);
            log.info(
                    "=== KIN METHOD RESULT === action={}, phase={}, chars={}, events={}, validationAccepted={}",
                    result.decision() != null ? result.decision().action() : null,
                    result.directive() != null ? result.directive().phase() : null,
                    result.aiResponse() != null ? result.aiResponse().length() : 0,
                    result.events().size(),
                    result.validation() != null ? result.validation().accepted() : null);
            var assistantMessage = finalizeTurn(
                    userId,
                    projectId,
                    result.aiResponse(),
                    result.decision(),
                    result.consultingReport(),
                    result.events());
            return ChatResponse.builder()
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .content(result.aiResponse())
                    .tokensUsed(assistantMessage.getTokensUsed())
                    .action(detectAction(userId, projectId, request.getContent()))
                    .build();
        } finally {
            clearReservation();
        }
    }

    @Override
    public SseEmitter processMessageStream(UUID userId, UUID projectId, ChatRequest request) {
        var project = findProject(userId, projectId);
        if (isBlocked(request.getContent())) {
            saveUserMessage(userId, projectId, request.getContent());
            return blockedEmitter();
        }
        var userMessage = saveUserMessage(userId, projectId, request.getContent());
        var history = loadHistoryForContext(userId, projectId);
        var streamAction = detectAction(userId, projectId, request.getContent());
        AiReservation reservation;
        try {
            reservation = reserveBudget(userId, request, history);
        } catch (AiBudgetExceededException e) {
            return budgetExceededEmitter(e.getMessage());
        }
        setReservation(reservation);
        var turn = new ConversationTurn(
                projectId,
                userId,
                request.getContent(),
                history,
                project.getTitle(),
                project.getDescription(),
                project.getCategory() != null ? project.getCategory().getCode() : null);

        log.info(
                "=== STREAMING AI RESPONSE === project={}, userId={}, historySize={}",
                projectId,
                userId,
                history.size());

        var emitter = new SseEmitter(SSE_TIMEOUT);
        var fullContent = new StringBuilder();
        var emitterCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
        Disposable[] subscriptionHolder = new Disposable[1];
        var pipelineCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
        var keepAliveCancelled = new java.util.concurrent.atomic.AtomicBoolean(false);

        emitter.onCompletion(() -> {
            log.info("=== SSE EMITTER COMPLETED CALLBACK === projectId={}", projectId);
            keepAliveCancelled.set(true);
            cancelSubscription(subscriptionHolder, projectId);
        });
        emitter.onTimeout(() -> {
            log.warn("=== SSE EMITTER TIMEOUT === projectId={}, timeoutMillis={}", projectId, SSE_TIMEOUT);
            keepAliveCancelled.set(true);
            cancelSubscription(subscriptionHolder, projectId);
        });
        emitter.onError(t -> {
            log.warn(
                    "=== SSE EMITTER ERROR CALLBACK === projectId={}, errorType={}, message={}",
                    projectId,
                    t != null ? t.getClass().getSimpleName() : "null",
                    t != null ? t.getMessage() : "null");
            keepAliveCancelled.set(true);
            cancelSubscription(subscriptionHolder, projectId);
        });

        try {
            sendEvent(emitter, "started", Map.of("message", "Procesando tu mensaje..."), projectId);
        } catch (IOException e) {
            log.error("Failed to send started event, client may have disconnected. projectId={}", projectId, e);
            emitter.complete();
            clearReservation();
            return emitter;
        }

        Runnable pipelineTask = () -> {
            try {
                StreamingTurnOutcome outcome = conversationOrchestrator.orchestrateStreamWithOutcome(turn);
                pipelineCompleted.set(true);
                Flux<String> flux = outcome == null
                        ? Flux.error(new IllegalStateException("No se obtuvo flujo de respuesta"))
                        : outcome.flux();

                subscriptionHolder[0] = flux.subscribe(
                        token -> {
                            if (emitterCompleted.get()) {
                                log.debug("SSE token ignorado: emitter ya completado. projectId={}", projectId);
                                return;
                            }
                            fullContent.append(token);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(objectMapper.writeValueAsString(Map.of("token", token))));
                            } catch (IOException e) {
                                log.error(
                                        "Failed to send SSE event, client may have disconnected. projectId={}",
                                        projectId,
                                        e);
                                emitterCompleted.set(true);
                                cancelSubscription(subscriptionHolder, projectId);
                            }
                        },
                        error -> {
                            log.error(
                                    "=== SSE STREAM ERROR === projectId={}, errorType={}, message={}",
                                    projectId,
                                    error != null ? error.getClass().getSimpleName() : "null",
                                    error != null ? error.getMessage() : "null");
                            if (!emitterCompleted.compareAndSet(false, true)) {
                                return;
                            }
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(objectMapper.writeValueAsString(Map.of(
                                                "error",
                                                error != null ? error.getMessage() : "Unknown stream error"))));
                            } catch (IOException e) {
                                log.error("Failed to send SSE error event. projectId={}", projectId, e);
                            }
                            emitter.complete();
                        },
                        () -> {
                            var finalContent = fullContent.toString();
                            ChatMessageResponse assistantMessage = null;
                            log.info("=== AI RESPONSE RECEIVED === chars={}", finalContent.length());

                            try {
                                assistantMessage = finalizeTurn(
                                        userId,
                                        projectId,
                                        finalContent,
                                        outcome != null ? outcome.decision() : null,
                                        outcome != null ? outcome.consultingReport() : null,
                                        outcome != null ? outcome.events() : List.of());
                            } catch (Exception e) {
                                log.error(
                                        "=== SSE TURN FINALIZATION FAILED === projectId={}, contentChars={}, errorType={}, message={}",
                                        projectId,
                                        finalContent.length(),
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                                if (!emitterCompleted.compareAndSet(false, true)) {
                                    return;
                                }
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data(objectMapper.writeValueAsString(Map.of(
                                                    "error", "No se pudo finalizar la respuesta: " + e.getMessage()))));
                                } catch (IOException sendError) {
                                    log.error(
                                            "Failed to send finalization error event. projectId={}",
                                            projectId,
                                            sendError);
                                }
                                emitter.complete();
                                return;
                            }

                            String assistantMessageId = assistantMessage != null
                                    ? assistantMessage.getId().toString()
                                    : "";
                            int tokensUsed = assistantMessage != null ? assistantMessage.getTokensUsed() : 0;

                            if (!emitterCompleted.compareAndSet(false, true)) {
                                log.warn(
                                        "=== SSE EMITTER ALREADY COMPLETED — se omite done === projectId={}",
                                        projectId);
                                return;
                            }

                            log.info(
                                    "=== SSE DONE SEND START === projectId={}, contentChars={}, assistantMessageId={}",
                                    projectId,
                                    finalContent.length(),
                                    assistantMessageId);
                            boolean doneSent = false;
                            try {
                                var donePayload = new java.util.LinkedHashMap<String, Object>();
                                donePayload.put("done", true);
                                donePayload.put(
                                        "userMessageId", userMessage.getId().toString());
                                donePayload.put(
                                        "assistantMessageId", assistantMessageId != null ? assistantMessageId : "");
                                donePayload.put("content", finalContent);
                                donePayload.put("tokensUsed", tokensUsed);
                                if (streamAction != null) {
                                    donePayload.put(
                                            "action",
                                            Map.of(
                                                    "type", streamAction.type(),
                                                    "format",
                                                            streamAction
                                                                    .format()
                                                                    .name(),
                                                    "templateDocumentId",
                                                            streamAction.templateDocumentId() == null
                                                                    ? null
                                                                    : streamAction
                                                                            .templateDocumentId()
                                                                            .toString(),
                                                    "templateDocumentName", streamAction.templateDocumentName()));
                                }
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(objectMapper.writeValueAsString(donePayload)));
                                doneSent = true;
                            } catch (Exception e) {
                                log.error(
                                        "=== SSE DONE SEND FAILED === projectId={}, errorType={}, message={}",
                                        projectId,
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                            }

                            if (!doneSent) {
                                try {
                                    var fallbackPayload = new java.util.LinkedHashMap<String, Object>();
                                    fallbackPayload.put("done", true);
                                    fallbackPayload.put("content", finalContent);
                                    fallbackPayload.put(
                                            "userMessageId", userMessage.getId().toString());
                                    fallbackPayload.put(
                                            "assistantMessageId", assistantMessageId != null ? assistantMessageId : "");
                                    fallbackPayload.put("tokensUsed", tokensUsed);
                                    if (streamAction != null) {
                                        fallbackPayload.put(
                                                "action",
                                                Map.of(
                                                        "type",
                                                        streamAction.type(),
                                                        "format",
                                                        streamAction.format().name(),
                                                        "templateDocumentId",
                                                        streamAction.templateDocumentId() == null
                                                                ? null
                                                                : streamAction
                                                                        .templateDocumentId()
                                                                        .toString(),
                                                        "templateDocumentName",
                                                        streamAction.templateDocumentName()));
                                    }
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data(objectMapper.writeValueAsString(fallbackPayload)));
                                    doneSent = true;
                                } catch (Exception e) {
                                    log.error(
                                            "=== SSE DONE SEND FALLBACK FAILED === projectId={}, errorType={}, message={}",
                                            projectId,
                                            e.getClass().getSimpleName(),
                                            e.getMessage());
                                }
                            }

                            log.info(
                                    "=== SSE DONE SEND SUCCESS === projectId={}, contentChars={}, assistantMessageId={}, sent={}",
                                    projectId,
                                    finalContent.length(),
                                    assistantMessageId,
                                    doneSent);

                            try {
                                emitter.complete();
                            } finally {
                                log.info("=== SSE EMITTER COMPLETE === projectId={}", projectId);
                            }
                        });
            } catch (Exception e) {
                log.error(
                        "=== PIPELINE EXECUTION ERROR === projectId={}, errorType={}, message={}",
                        projectId,
                        e.getClass().getSimpleName(),
                        e.getMessage());
                if (!emitterCompleted.compareAndSet(false, true)) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(
                                    Map.of("error", "Error interno del pipeline: " + e.getMessage()))));
                } catch (IOException ex) {
                    log.error("Failed to send pipeline error event. projectId={}", projectId, ex);
                }
                emitter.complete();
            }
        };

        if (useSynchronousExecutor) {
            pipelineTask.run();
        } else {
            pipelineExecutor.submit(pipelineTask);
        }

        if (!useSynchronousExecutor) {
            keepAliveExecutor(emitter, emitterCompleted, pipelineCompleted, keepAliveCancelled, projectId);
        }

        return emitter;
    }

    private void keepAliveExecutor(
            SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean emitterCompleted,
            java.util.concurrent.atomic.AtomicBoolean pipelineCompleted,
            java.util.concurrent.atomic.AtomicBoolean keepAliveCancelled,
            UUID projectId) {
        new Thread(
                        () -> {
                            while (!emitterCompleted.get() && !keepAliveCancelled.get()) {
                                try {
                                    Thread.sleep(15_000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                if (emitterCompleted.get() || keepAliveCancelled.get()) {
                                    break;
                                }
                                if (pipelineCompleted.get()) {
                                    break;
                                }
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("keepalive")
                                            .data(objectMapper.writeValueAsString(Map.of("status", "processing"))));
                                } catch (IOException e) {
                                    log.debug(
                                            "Keep-alive send failed, client may have disconnected. projectId={}",
                                            projectId);
                                    break;
                                }
                            }
                        },
                        "kin-keepalive-" + projectId)
                .start();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> data, UUID projectId)
            throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(objectMapper.writeValueAsString(data)));
    }

    private boolean isBlocked(String content) {
        if (promptGuardrail == null || content == null || content.isBlank()) {
            return false;
        }
        boolean blocked = promptGuardrail.analyze(content).blocked();
        if (blocked) {
            log.warn("=== GUARDRAIL BLOCKED === input contained prompt injection signals");
        }
        return blocked;
    }

    /** Cancela la suscripción al flux cuando el emitter ya terminó (timeout/complete/error). */
    private void cancelSubscription(Disposable[] subscriptionHolder, UUID projectId) {
        if (subscriptionHolder == null || subscriptionHolder[0] == null) {
            return;
        }
        Disposable disposable = subscriptionHolder[0];
        if (!disposable.isDisposed()) {
            disposable.dispose();
            log.info("=== SSE SUBSCRIPTION CANCELLED === projectId={}", projectId);
        }
    }

    /**
     * Reserva atómicamente el presupuesto de IA estimado para el turno ANTES
     * de ejecutar el pipeline (y por tanto antes de llamar a DeepSeek). Si el
     * control de costo está desactivado o no hay precios configurados devuelve
     * {@code null} (sin gate). Si el presupuesto es insuficiente lanza
     * {@link AiBudgetExceededException} (HTTP 403).
     */
    private AiReservation reserveBudget(UUID userId, ChatRequest request, List<Message> history) {
        if (budgetControlService == null || subscriptionValidator == null) {
            return null;
        }
        var plan = subscriptionValidator.getCurrentPlan(userId);
        return budgetControlService.reserve(userId, plan, request.getContent(), history);
    }

    private void clearReservation() {
        if (reservationContext != null) {
            reservationContext.clear();
        }
    }

    private void setReservation(AiReservation reservation) {
        if (reservationContext != null && reservation != null) {
            reservationContext.set(reservation);
        }
    }

    /** Emitter SSE que muestra el mensaje amigable de límite de IA alcanzado. */
    private SseEmitter budgetExceededEmitter(String message) {
        var emitter = new SseEmitter(SSE_TIMEOUT);
        String friendly =
                message == null || message.isBlank() ? AiBudgetControlService.BUDGET_EXCEEDED_MESSAGE : message;
        try {
            emitter.send(
                    SseEmitter.event().name("token").data(objectMapper.writeValueAsString(Map.of("token", friendly))));
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "done",
                            true,
                            "userMessageId",
                            "",
                            "assistantMessageId",
                            "",
                            "content",
                            friendly,
                            "tokensUsed",
                            0))));
        } catch (IOException e) {
            log.error("Failed to emit AI budget exceeded event", e);
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    private void persistReportIfGenerated(UUID projectId, ConversationDecision decision, ConsultingReport report) {
        if (decision != null && decision.action() == ConversationDecision.Action.REPORT && report != null) {
            try {
                reportRepository.save(projectId, report);
            } catch (Exception e) {
                log.error("Failed to persist consulting report for project={}", projectId, e);
            }
        }
    }

    private SseEmitter blockedEmitter() {
        var emitter = new SseEmitter(SSE_TIMEOUT);
        try {
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data(objectMapper.writeValueAsString(Map.of("token", BLOCKED_MESSAGE))));
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "done",
                            true,
                            "userMessageId",
                            "",
                            "assistantMessageId",
                            "",
                            "content",
                            BLOCKED_MESSAGE,
                            "tokensUsed",
                            0))));
        } catch (IOException e) {
            log.error("Failed to emit guardrail blocked event", e);
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    private com.kinplatform.project.Project findProject(UUID userId, UUID projectId) {
        var project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project does not belong to this user");
        }
        return project;
    }

    private ExportAction detectAction(UUID userId, UUID projectId, String message) {
        if (exportChatIntentService == null) {
            return null;
        }
        return exportChatIntentService.detect(userId, projectId, message);
    }

    private ChatMessageResponse saveUserMessage(UUID userId, UUID projectId, String content) {
        var request = new SaveMessageRequest();
        request.setRole(MessageRole.USER);
        request.setContent(content);
        return chatService.saveMessage(userId, projectId, request);
    }

    private ChatMessageResponse saveAssistantMessage(UUID userId, UUID projectId, String content) {
        var request = new SaveMessageRequest();
        request.setRole(MessageRole.ASSISTANT);
        request.setContent(content);
        return chatService.saveMessage(userId, projectId, request);
    }

    /**
     * Finaliza el turno. En producción delega en
     * {@link ChatTurnFinalizationService} (transacción corta que persiste
     * mensaje asistente + reporte + eventos outbox de forma atómica). Cuando el
     * servicio de finalización no está inyectado (tests/constructores legacy)
     * conserva el comportamiento previo: guardar el asistente y persistir el
     * reporte sin publicar eventos.
     */
    private ChatMessageResponse finalizeTurn(
            UUID userId,
            UUID projectId,
            String assistantContent,
            ConversationDecision decision,
            ConsultingReport consultingReport,
            List<DomainEvent> events) {
        if (finalizationService != null) {
            return finalizationService.finalizeTurn(
                    userId, projectId, assistantContent, decision, consultingReport, events);
        }
        var assistantMessage = saveAssistantMessage(userId, projectId, assistantContent);
        persistReportIfGenerated(projectId, decision, consultingReport);
        return assistantMessage;
    }

    private List<Message> loadHistoryForContext(UUID userId, UUID projectId) {
        var history = chatService.getConversationHistory(userId, projectId);
        var messages = new ArrayList<Message>(history.size());
        for (var msg : history) {
            messages.add(new Message(msg.getRole().name(), msg.getContent()));
        }
        return messages;
    }
}



