package com.kinplatform.kin;

import com.kinplatform.common.context.CompletenessEvaluation;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import com.kinplatform.platform.scoring.ScoreResult;

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




