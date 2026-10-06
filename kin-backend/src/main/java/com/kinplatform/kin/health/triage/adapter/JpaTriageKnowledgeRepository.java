package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.common.engine.DeterministicId;
import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link TriageKnowledgeRepository} (ADR-028).
 *
 * <p>Carga el catálogo completo de triaje (síntomas, condiciones y relaciones)
 * desde las tablas {@code symptoms}, {@code conditions} y
 * {@code symptom_condition_relations}, y lo expone como {@link TriageCatalog}
 * (snapshot inmutable) al motor. {@code applyUpdate} realiza un upsert
 * idempotente de una {@link CatalogUpdate} (fase profesional): los elementos
 * nuevos se insertan y los existentes se actualizan, sin duplicar.</p>
 */
@Component
public class JpaTriageKnowledgeRepository implements TriageKnowledgeRepository {

    private final SymptomJpaRepository symptomRepository;
    private final ConditionJpaRepository conditionRepository;
    private final SymptomConditionRelationJpaRepository relationRepository;

    public JpaTriageKnowledgeRepository(
            SymptomJpaRepository symptomRepository,
            ConditionJpaRepository conditionRepository,
            SymptomConditionRelationJpaRepository relationRepository) {
        this.symptomRepository = symptomRepository;
        this.conditionRepository = conditionRepository;
        this.relationRepository = relationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TriageCatalog loadCatalog() {
        var symptoms = symptomRepository.findAll().stream()
                .map(TriageMapper::toSymptom)
                .toList();
        var conditions = conditionRepository.findAll().stream()
                .map(TriageMapper::toCondition)
                .toList();
        var relations = relationRepository.findAll().stream()
                .map(TriageMapper::toRelation)
                .toList();
        return TriageCatalog.of(symptoms, conditions, relations);
    }

    @Override
    @Transactional
    public CatalogUpdateResult applyUpdate(CatalogUpdate update) {
        if (update == null || update.isEmpty()) {
            return CatalogUpdateResult.empty(update == null ? "" : update.source());
        }
        int symptomsAdded = upsertSymptoms(update.symptoms());
        int conditionsAdded = upsertConditions(update.conditions());
        int relationsAdded = upsertRelations(update.relations());
        return CatalogUpdateResult.of(symptomsAdded, conditionsAdded, relationsAdded, update.source());
    }

    private int upsertSymptoms(List<com.kinplatform.kin.health.triage.domain.Symptom> symptoms) {
        int added = 0;
        Map<String, UUID> byName = indexSymptomNames();
        for (var symptom : symptoms) {
            if (symptom == null) {
                continue;
            }
            UUID id = symptom.id() != null ? symptom.id() : byName.get(symptom.name());
            SymptomEntity entity;
            if (id != null && symptomRepository.existsById(id)) {
                entity = symptomRepository.findById(id).orElse(null);
            } else if (id == null && byName.containsKey(symptom.name())) {
                entity = symptomRepository.findByName(symptom.name()).orElse(null);
            } else {
                entity = null;
            }
            if (entity == null) {
                entity = new SymptomEntity();
                entity.setId(
                        id != null ? id : DeterministicId.from("triage-symptom", symptom.name(), symptom.icdCode()));
                entity.setCreatedAt(java.time.OffsetDateTime.now());
                added++;
            }
            entity.setName(symptom.name());
            entity.setDescription(symptom.description());
            entity.setIcdCode(symptom.icdCode());
            entity.setAliases(symptom.aliases());
            symptomRepository.save(entity);
            byName.put(symptom.name(), entity.getId());
        }
        return added;
    }

    private int upsertConditions(List<com.kinplatform.kin.health.triage.domain.Condition> conditions) {
        int added = 0;
        Map<String, UUID> byName = indexConditionNames();
        for (var condition : conditions) {
            if (condition == null) {
                continue;
            }
            UUID id = condition.id() != null ? condition.id() : byName.get(condition.name());
            ConditionEntity entity;
            if (id != null && conditionRepository.existsById(id)) {
                entity = conditionRepository.findById(id).orElse(null);
            } else if (id == null && byName.containsKey(condition.name())) {
                entity = conditionRepository.findByName(condition.name()).orElse(null);
            } else {
                entity = null;
            }
            if (entity == null) {
                entity = new ConditionEntity();
                entity.setId(
                        id != null
                                ? id
                                : DeterministicId.from("triage-condition", condition.name(), condition.icdCode()));
                entity.setCreatedAt(java.time.OffsetDateTime.now());
                added++;
            }
            entity.setName(condition.name());
            entity.setDescription(condition.description());
            entity.setIcdCode(condition.icdCode());
            entity.setSeverity(condition.severity());
            entity.setUrgency(condition.urgency());
            entity.setRecommendation(condition.recommendation());
            entity.setValidationStatus(condition.validationStatus());
            conditionRepository.save(entity);
            byName.put(condition.name(), entity.getId());
        }
        return added;
    }

    private int upsertRelations(List<SymptomConditionRelation> relations) {
        int added = 0;
        for (var relation : relations) {
            if (relation == null || relation.symptomId() == null || relation.conditionId() == null) {
                continue;
            }
            var key = new SymptomConditionRelationEntity.SymptomConditionRelationId(
                    relation.symptomId(), relation.conditionId());
            boolean exists = relationRepository.existsById(key);
            SymptomConditionRelationEntity entity;
            if (exists) {
                entity = relationRepository.getReferenceById(key);
            } else {
                entity = new SymptomConditionRelationEntity();
                entity.setId(key);
                added++;
            }
            entity.setWeight(relation.weight());
            entity.setRequired(relation.required());
            relationRepository.save(entity);
        }
        return added;
    }

    private Map<String, UUID> indexSymptomNames() {
        var index = new HashMap<String, UUID>();
        for (SymptomEntity entity : symptomRepository.findAll()) {
            index.put(entity.getName(), entity.getId());
        }
        return index;
    }

    private Map<String, UUID> indexConditionNames() {
        var index = new HashMap<String, UUID>();
        for (ConditionEntity entity : conditionRepository.findAll()) {
            index.put(entity.getName(), entity.getId());
        }
        return index;
    }
}

