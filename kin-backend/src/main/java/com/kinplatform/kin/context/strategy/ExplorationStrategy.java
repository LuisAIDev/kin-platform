package com.kinplatform.kin.context.strategy;

import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;

public interface ExplorationStrategy {

    ConversationDecision decide(ProjectContext context, CompletenessEvaluation evaluation);
}
