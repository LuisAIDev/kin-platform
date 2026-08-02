package com.kinplatform.ai.interview.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.interview.InterviewRepository;
import com.kinplatform.kin.interview.InterviewState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JPA del puerto {@link InterviewRepository} (ADR-015).
 *
 * <p>Persiste el {@link InterviewState} de cada proyecto como JSON en la tabla
 * {@code interview_state}, de forma durable entre reinicios. La estructura
 * serializada es un DTO propio del adaptador (no el objeto de dominio), por lo
 * que el formato de persistencia no acopla el dominio.</p>
 */
@Component
public class JpaInterviewRepository implements InterviewRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaInterviewRepository.class);

    private final InterviewStateJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final InterviewStateMapper mapper;

    public JpaInterviewRepository(InterviewStateJpaRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, new InterviewStateMapper());
    }

    public JpaInterviewRepository(InterviewStateJpaRepository repository, ObjectMapper objectMapper,
                                  InterviewStateMapper mapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InterviewState> find(UUID projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return repository.findById(projectId).map(this::fromEntity);
    }

    @Override
    @Transactional
    public void save(InterviewState state) {
        if (state == null) {
            throw new IllegalArgumentException("state no puede ser null");
        }
        var entity = repository.findById(state.projectId()).orElseGet(InterviewStateEntity::new);
        entity.setProjectId(state.projectId());
        entity.setStateData(toJson(mapper.toData(state)));
        repository.save(entity);
    }

    private InterviewState fromEntity(InterviewStateEntity entity) {
        return mapper.toDomain(entity.getProjectId(), fromJson(entity.getStateData()));
    }

    private String toJson(InterviewStateData dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize InterviewState", e);
        }
    }

    private InterviewStateData fromJson(String json) {
        try {
            return objectMapper.readValue(json, InterviewStateData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize InterviewState", e);
        }
    }
}
