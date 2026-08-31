package com.kinplatform.kin.health.automation.port;

import com.kinplatform.kin.health.automation.domain.AutomationRule;
import com.kinplatform.kin.health.automation.domain.TriggerEvent;
import com.kinplatform.kin.health.automation.domain.RuleExecutionLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomationRuleRepository {

    AutomationRule save(AutomationRule rule);

    Optional<AutomationRule> findById(UUID id);

    List<AutomationRule> findByCreatedBy(UUID physicianId);

    List<AutomationRule> findByTriggerEventAndEnabled(TriggerEvent event, boolean enabled);

    long countByCreatedByAndEnabled(UUID physicianId, boolean enabled);

    AutomationRule update(AutomationRule rule);

    void deleteById(UUID id);
}