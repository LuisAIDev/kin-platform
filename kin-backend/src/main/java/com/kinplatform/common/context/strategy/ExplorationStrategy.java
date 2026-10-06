package com.kinplatform.common.context.strategy;

import com.kinplatform.common.context.CompletenessEvaluation;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.decision.ConversationDecision;

public interface ExplorationStrategy {

    ConversationDecision decide(ProjectContext context, CompletenessEvaluation evaluation);
}


