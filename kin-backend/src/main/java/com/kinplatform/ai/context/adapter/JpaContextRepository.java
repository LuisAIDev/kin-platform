package com.kinplatform.ai.context.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JPA del puerto {@link ContextRepository}.
 *
 * <p>Persiste el {@code ProjectContext} de cada proyecto como JSON en la tabla
 * {@code project_context}, de forma durable entre reinicios. La estructura
 * serializada es un DTO propio del adaptador (no el objeto de dominio), por lo
 * que el formato de persistencia no acopla el dominio.</p>
 */
@Component
public class JpaContextRepository implements ContextRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaContextRepository.class);

    private final ProjectContextJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaContextRepository(ProjectContextJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ProjectContext findOrCreate(UUID projectId, String projectTitle,
                                       String projectDescription, String projectCategory) {
        return repository.findById(projectId)
            .map(this::fromEntity)
            .orElseGet(() -> {
                log.info("Initialized context for project {} from Project entity", projectId);
                var created = ProjectContext.fromProject(projectTitle, projectDescription, projectCategory);
                save(projectId, created);
                return created;
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectContext> find(UUID projectId) {
        return repository.findById(projectId).map(this::fromEntity);
    }

    @Override
    @Transactional
    public void save(UUID projectId, ProjectContext context) {
        var entity = repository.findById(projectId).orElseGet(ProjectContextEntity::new);
        entity.setProjectId(projectId);
        entity.setContextData(toJson(toData(context)));
        repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(UUID projectId) {
        repository.deleteById(projectId);
    }

    private ProjectContext fromEntity(ProjectContextEntity entity) {
        return toDomain(fromJson(entity.getContextData()));
    }

    private ProjectContextData toData(ProjectContext context) {
        var data = new HashMap<String, String>();
        for (var dim : AnalyzedDimension.values()) {
            var value = context.value(dim);
            if (value != null) {
                data.put(dim.name(), value);
            }
        }
        var covered = context.coveredDimensions().stream().map(Enum::name).toList();
        return new ProjectContextData(
            data, covered, context.currentDecision(),
            context.exchangeCount(), context.reportGenerated());
    }

    private ProjectContext toDomain(ProjectContextData dto) {
        var data = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        for (var entry : dto.data().entrySet()) {
            data.put(AnalyzedDimension.valueOf(entry.getKey()), entry.getValue());
        }
        var covered = EnumSet.noneOf(AnalyzedDimension.class);
        for (var name : dto.dimensionsCovered()) {
            covered.add(AnalyzedDimension.valueOf(name));
        }
        return ProjectContext.restore(data, covered, dto.decision(),
            dto.exchangeCount(), dto.reportGenerated());
    }

    private String toJson(ProjectContextData dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ProjectContext", e);
        }
    }

    private ProjectContextData fromJson(String json) {
        try {
            return objectMapper.readValue(json, ProjectContextData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ProjectContext", e);
        }
    }

    private record ProjectContextData(
        Map<String, String> data,
        List<String> dimensionsCovered,
        ConversationDecision decision,
        int exchangeCount,
        boolean reportGenerated
    ) {}
}
