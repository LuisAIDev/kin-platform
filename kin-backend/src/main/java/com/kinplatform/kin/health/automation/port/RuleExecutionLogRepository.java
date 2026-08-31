package com.kinplatform.kin.health.automation.port;

import com.kinplatform.kin.health.automation.domain.RuleExecutionLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleExecutionLogRepository {

    RuleExecutionLog save(RuleExecutionLog log);

    Optional<RuleExecutionLog> findById(UUID id);

    List<RuleExecutionLog> findByRuleId(UUID ruleId);

    List<RuleExecutionLog> findByEventId(UUID eventId);

    List<RuleExecutionLog> findByExecutedFalseOrderByTriggeredAtDesc();

    long countByRuleIdAndExecuted(UUID ruleId, boolean executed);
}