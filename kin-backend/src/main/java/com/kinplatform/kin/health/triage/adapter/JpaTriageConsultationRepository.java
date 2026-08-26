package com.kinplatform.kin.health.triage.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link TriageConsultationRepository} (ADR-028).
 *
 * <p>Persiste cada consulta de triaje en la tabla {@code triage_consultations}
 * serializando síntomas y resultados como JSON. El historial se consulta por
 * usuario (aislamiento de datos de salud: solo el propietario accede a sus
 * consultas).</p>
 */
@Component
public class JpaTriageConsultationRepository implements TriageConsultationRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaTriageConsultationRepository.class);

    private final TriageConsultationJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaTriageConsultationRepository(TriageConsultationJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TriageConsultation save(TriageConsultation consultation) {
        if (consultation == null) {
            throw new IllegalArgumentException("consultation no puede ser null");
        }
        var entity = repository.findById(consultation.id()).orElseGet(TriageConsultationEntity::new);
        entity.setId(consultation.id());
        entity.setUserId(consultation.userId());
        entity.setSymptoms(toJson(consultation.symptoms()));
        entity.setResults(toJson(consultation.results()));
        TriageConsultationEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageConsultation> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TriageConsultation> findByUserId(
            UUID userId, org.springframework.data.domain.Pageable pageable) {
        if (userId == null) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TriageConsultation> findByIdAndUserId(UUID id, UUID userId) {
        if (id == null || userId == null) {
            return Optional.empty();
        }
        return repository.findById(id).filter(e -> userId.equals(e.getUserId())).map(this::toDomain);
    }

    private TriageConsultation toDomain(TriageConsultationEntity entity) {
        if (entity == null) {
            return null;
        }
        return TriageConsultation.of(
                entity.getId(),
                entity.getUserId(),
                fromJson(entity.getSymptoms(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}),
                fromJson(
                        entity.getResults(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<TriageConditionResult>>() {}),
                entity.getCreatedAt());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize triage consultation", e);
        }
    }

    private <T> T fromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize triage consultation JSON, returning empty", e);
            return null;
        }
    }
}
