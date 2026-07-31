package com.kinplatform.ai.context;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextAnalyzerPort;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProjectContextService {

    private static final Logger log = LoggerFactory.getLogger(ProjectContextService.class);

    private final ConcurrentHashMap<UUID, ProjectContext> contexts = new ConcurrentHashMap<>();
    private final ContextAnalyzerPort analyzer;
    private final ProjectRepository projectRepository;

    public ProjectContextService(ContextAnalyzerPort analyzer, ProjectRepository projectRepository) {
        this.analyzer = analyzer;
        this.projectRepository = projectRepository;
    }

    public ProjectContext analyzeMessage(UUID projectId, String message) {
        var context = getOrCreate(projectId);
        var result = analyzer.analyze(message, context);
        if (!result.isEmpty()) {
            context.update(result);
            log.info("Context updated for project {} — known dimensions: {}/{}",
                    projectId, context.knownDimensionsCount(), AnalyzedDimension.values().length);
        }
        return context;
    }

    public ProjectContext getContext(UUID projectId) {
        return getOrCreate(projectId);
    }

    public void resetContext(UUID projectId) {
        contexts.remove(projectId);
        log.info("Context reset for project {}", projectId);
    }

    private ProjectContext getOrCreate(UUID projectId) {
        return contexts.computeIfAbsent(projectId, id -> {
            var project = projectRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
            log.info("Initialized context for project {} from Project entity", id);
            return ProjectContext.fromProject(
                    project.getTitle(), project.getDescription(),
                    project.getCategory() != null ? project.getCategory().name() : null);
        });
    }
}
