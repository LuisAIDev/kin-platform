package com.kinplatform.kin.health.automation.adapter;

import com.kinplatform.kin.health.automation.domain.RuleExecutionLog;
import com.kinplatform.kin.health.automation.port.RuleExecutionLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaRuleExecutionLogRepository implements RuleExecutionLogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public RuleExecutionLog save(RuleExecutionLog log) {
        RuleExecutionLogEntity entity = toEntity(log);
        entityManager.persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<RuleExecutionLog> findById(UUID id) {
        return entityManager.createQuery(
                "SELECT e FROM RuleExecutionLogEntity e WHERE e.id = :id", RuleExecutionLogEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public List<RuleExecutionLog> findByRuleId(UUID ruleId) {
        return entityManager.createQuery(
                "SELECT e FROM RuleExecutionLogEntity e WHERE e.ruleId = :ruleId", RuleExecutionLogEntity.class)
                .setParameter("ruleId", ruleId)
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RuleExecutionLog> findByEventId(UUID eventId) {
        return entityManager.createQuery(
                "SELECT e FROM RuleExecutionLogEntity e WHERE e.eventId = :eventId", RuleExecutionLogEntity.class)
                .setParameter("eventId", eventId)
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RuleExecutionLog> findByExecutedFalseOrderByTriggeredAtDesc() {
        return entityManager.createQuery(
                "SELECT e FROM RuleExecutionLogEntity e WHERE e.executed = false ORDER BY e.triggeredAt DESC", RuleExecutionLogEntity.class)
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByRuleIdAndExecuted(UUID ruleId, boolean executed) {
        return entityManager.createQuery(
                "SELECT COUNT(e) FROM RuleExecutionLogEntity e WHERE e.ruleId = :ruleId AND e.executed = :executed", Long.class)
                .setParameter("ruleId", ruleId)
                .setParameter("executed", executed)
                .getSingleResult();
    }

    static RuleExecutionLog toDomain(RuleExecutionLogEntity e) {
        return new RuleExecutionLog(
                e.getId(),
                e.getRuleId(),
                e.getEventId(),
                e.getTriggeredAt(),
                e.isExecuted(),
                e.getError(),
                e.getDetails());
    }

    static RuleExecutionLogEntity toEntity(RuleExecutionLog log) {
        return RuleExecutionLogEntity.builder()
                .id(log.id())
                .ruleId(log.ruleId())
                .eventId(log.eventId())
                .triggeredAt(log.triggeredAt())
                .executed(log.executed())
                .error(log.error())
                .details(log.details())
                .build();
    }
}