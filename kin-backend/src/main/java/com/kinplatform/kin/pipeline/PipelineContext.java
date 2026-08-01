package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.reporting.RecommendationResult;
import com.kinplatform.kin.reporting.opportunity.OpportunityResult;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.risk.RiskResult;
import com.kinplatform.kin.scoring.ScoreResult;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PipelineContext {

    private final UUID projectId;
    private final UUID userId;
    private final String userMessage;
    private final List<com.kinplatform.kin.context.Message> history;
    private final String projectTitle;
    private final String projectDescription;
    private final String projectCategory;

    private ProjectContext projectContext;
    private CompletenessEvaluation evaluation;
    private ConversationDecision decision;
    private String aiResponse;
    private ScoreResult scoreResult;
    private RecommendationResult recommendationResult;
    private RiskResult riskResult;
    private OpportunityResult opportunityResult;
    private ConsultingReport consultingReport;
    private final List<DomainEvent> events = new ArrayList<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, EngineResult> engineResults = new HashMap<>();
    private boolean completed;
    private String currentStage;
    private boolean streaming;
    private Flux<String> aiResponseFlux;
    private TurnDirective turnDirective;
    private ResponseValidation responseValidation;

    public PipelineContext(UUID projectId, UUID userId, String userMessage,
                           List<com.kinplatform.kin.context.Message> history,
                           String projectTitle, String projectDescription, String projectCategory) {
        this.projectId = projectId;
        this.userId = userId;
        this.userMessage = userMessage;
        this.history = history;
        this.projectTitle = projectTitle;
        this.projectDescription = projectDescription;
        this.projectCategory = projectCategory;
    }

    public UUID projectId() { return projectId; }
    public UUID userId() { return userId; }
    public String userMessage() { return userMessage; }
    public List<com.kinplatform.kin.context.Message> history() { return history; }
    public String projectTitle() { return projectTitle; }
    public String projectDescription() { return projectDescription; }
    public String projectCategory() { return projectCategory; }

    public ProjectContext projectContext() { return projectContext; }
    public void projectContext(ProjectContext ctx) { this.projectContext = ctx; }

    public CompletenessEvaluation evaluation() { return evaluation; }
    public void evaluation(CompletenessEvaluation e) { this.evaluation = e; }

    public ConversationDecision decision() { return decision; }
    public void decision(ConversationDecision d) { this.decision = d; }

    public String aiResponse() { return aiResponse; }
    public void aiResponse(String r) { this.aiResponse = r; }

    public ScoreResult scoreResult() { return scoreResult; }
    public void scoreResult(ScoreResult s) { this.scoreResult = s; }

    public RecommendationResult recommendationResult() { return recommendationResult; }
    public void recommendationResult(RecommendationResult r) { this.recommendationResult = r; }

    public RiskResult riskResult() { return riskResult; }
    public void riskResult(RiskResult r) { this.riskResult = r; }

    public OpportunityResult opportunityResult() { return opportunityResult; }
    public void opportunityResult(OpportunityResult r) { this.opportunityResult = r; }

    public ConsultingReport consultingReport() { return consultingReport; }
    public void consultingReport(ConsultingReport r) { this.consultingReport = r; }

    public void setEngineResult(String engineName, EngineResult result) { engineResults.put(engineName, result); }
    @SuppressWarnings("unchecked")
    public <T extends EngineResult> T engineResult(String engineName) { return (T) engineResults.get(engineName); }
    public Map<String, EngineResult> engineResults() { return Map.copyOf(engineResults); }

    public List<DomainEvent> events() { return List.copyOf(events); }
    public void addEvent(DomainEvent event) { events.add(event); }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }

    public boolean completed() { return completed; }
    public void markCompleted() { this.completed = true; }

    public String currentStage() { return currentStage; }
    public void currentStage(String stage) { this.currentStage = stage; }

    public boolean streaming() { return streaming; }
    public void streaming(boolean streaming) { this.streaming = streaming; }

    public Flux<String> aiResponseFlux() { return aiResponseFlux; }
    public void aiResponseFlux(Flux<String> flux) { this.aiResponseFlux = flux; }

    public TurnDirective turnDirective() { return turnDirective; }
    public void turnDirective(TurnDirective directive) { this.turnDirective = directive; }

    public ResponseValidation responseValidation() { return responseValidation; }
    public void responseValidation(ResponseValidation validation) { this.responseValidation = validation; }
}
