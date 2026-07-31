package com.kinplatform.kin;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.scoring.ScoreResult;

import java.util.List;

public record KinMethodResult(
    ProjectContext projectContext,
    CompletenessEvaluation evaluation,
    ConversationDecision decision,
    String aiResponse,
    ScoreResult score,
    List<DomainEvent> events,
    ConsultingReport consultingReport
) {
}
