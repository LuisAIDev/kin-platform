package com.kinplatform.kin.health.automation.adapter;

import com.kinplatform.kin.health.automation.domain.AutomationRule;
import com.kinplatform.kin.health.automation.domain.TriggerEvent;
import com.kinplatform.kin.health.automation.port.AutomationRuleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAutomationRuleRepository implements AutomationRuleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public AutomationRule save(AutomationRule rule) {
        AutomationRuleEntity entity = toEntity(rule);
        entityManager.persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<AutomationRule> findById(UUID id) {
        return entityManager
                .createQuery("SELECT e FROM AutomationRuleEntity e WHERE e.id = :id", AutomationRuleEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(log -> toDomain(log));
    }

    @Override
    public List<AutomationRule> findByCreatedBy(UUID physicianId) {
        return entityManager
                .createQuery(
                        "SELECT e FROM AutomationRuleEntity e WHERE e.createdBy = :physicianId AND e.enabled = true",
                        AutomationRuleEntity.class)
                .setParameter("physicianId", physicianId)
                .getResultList()
                .stream()
                .map(log -> toDomain(log))
                .toList();
    }

    @Override
    public List<AutomationRule> findByTriggerEventAndEnabled(TriggerEvent event, boolean enabled) {
        return entityManager
                .createQuery(
                        "SELECT e FROM AutomationRuleEntity e WHERE e.triggerEvent = :event AND e.enabled = :enabled",
                        AutomationRuleEntity.class)
                .setParameter("event", event)
                .setParameter("enabled", enabled)
                .getResultList()
                .stream()
                .map(log -> toDomain(log))
                .toList();
    }

    @Override
    public long countByCreatedByAndEnabled(UUID physicianId, boolean enabled) {
        return entityManager
                .createQuery(
                        "SELECT COUNT(e) FROM AutomationRuleEntity e WHERE e.createdBy = :physicianId AND e.enabled = :enabled",
                        Long.class)
                .setParameter("physicianId", physicianId)
                .setParameter("enabled", enabled)
                .getSingleResult();
    }

    @Override
    @Transactional
    public AutomationRule update(AutomationRule rule) {
        AutomationRuleEntity entity = entityManager.getReference(AutomationRuleEntity.class, rule.id());
        entity.setName(rule.name());
        entity.setDescription(rule.description());
        entity.setTriggerEvent(rule.triggerEvent());
        entity.setConditions(rule.conditions());
        entity.setAction(rule.action());
        entity.setActionParams(rule.actionParams());
        entity.setEnabled(rule.enabled());
        entity.setUpdatedAt(OffsetDateTime.now());
        return toDomain(entity);
    }

    @Override
    public void deleteById(UUID id) {
        AutomationRuleEntity entity = entityManager.getReference(AutomationRuleEntity.class, id);
        entityManager.remove(entity);
    }

    static AutomationRule toDomain(AutomationRuleEntity e) {
        return AutomationRule.of(
                e.getName(),
                e.getDescription(),
                e.getTriggerEvent(),
                e.getConditions(),
                e.getAction(),
                e.getActionParams(),
                e.isEnabled(),
                e.getCreatedBy());
    }

    static AutomationRuleEntity toEntity(AutomationRule rule) {
        return AutomationRuleEntity.builder()
                .id(rule.id())
                .name(rule.name())
                .description(rule.description())
                .triggerEvent(rule.triggerEvent())
                .conditions(rule.conditions())
                .action(rule.action())
                .actionParams(rule.actionParams())
                .enabled(rule.enabled())
                .createdBy(rule.createdBy())
                .createdAt(rule.createdAt())
                .updatedAt(rule.updatedAt())
                .build();
    }
}
