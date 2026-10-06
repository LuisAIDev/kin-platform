package com.kinplatform.ai.enterprise.integration;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.kin.enterprise.integration.EnterpriseContextEnricher;
import com.kinplatform.kin.enterprise.integration.ResolvedContext;
import com.kinplatform.kin.enterprise.integration.ResolvedValue;
import com.kinplatform.kin.enterprise.integration.StructuredDatum;
import com.kinplatform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.projectinfo.ProjectStructuredInfoRepository;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link EnterpriseContextEnricher}: lee
 * {@code project_structured_info} y construye una nueva instancia de
 * {@code ProjectContext} rellenando solo las dimensiones pendientes. No toca la
 * persistencia ni el contexto original.
 */
@Service
public class DefaultEnterpriseContextEnricher implements EnterpriseContextEnricher {

    private final ProjectStructuredInfoRepository repository;

    public DefaultEnterpriseContextEnricher(ProjectStructuredInfoRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProjectContext enrich(UUID projectId, ProjectContext context) {
        ProjectContext base = context == null ? emptyContext() : context;
        var data = repository.findByProjectIdOrderBySectionAscKeyAsc(projectId).stream()
                .map(this::toDatum)
                .toList();
        ResolvedContext resolved = ResolvedContext.resolve(base, data);
        return overlay(base, resolved);
    }

    private ProjectContext overlay(ProjectContext base, ResolvedContext resolved) {
        Map<AnalyzedDimension, String> data = new EnumMap<>(AnalyzedDimension.class);
        Set<AnalyzedDimension> covered = EnumSet.noneOf(AnalyzedDimension.class);
        for (AnalyzedDimension dimension : AnalyzedDimension.values()) {
            ResolvedValue value = resolved.value(dimension);
            if (value.value() != null && !value.value().isBlank()) {
                data.put(dimension, value.value());
                covered.add(dimension);
            }
        }
        return ProjectContext.restore(
                data, covered, base.currentDecision(), base.exchangeCount(), base.reportGenerated());
    }

    private ProjectContext emptyContext() {
        return ProjectContext.restore(Map.of(), Set.of(), null, 0, false);
    }

    private StructuredDatum toDatum(ProjectStructuredInfo entity) {
        return new StructuredDatum(entity.getSection(), entity.getKey(), entity.getValue(), entity.getSourceType());
    }
}

