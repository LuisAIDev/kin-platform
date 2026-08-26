package com.kinplatform.kin.health.differential.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.engine.DeterministicId;
import com.kinplatform.kin.health.differential.domain.CatalogUpdate;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Parser del dataset del diagnóstico diferencial → {@link CatalogUpdate}
 * (ADR-029).
 *
 * <p>Infraestructura (Jackson): convierte el contenido JSON de un
 * {@code KnowledgeSource} en una {@link CatalogUpdate} de dominio. Los nombres
 * de condición se resuelven a IDs mediante el {@code conditionResolver}
 * (proveído con el catálogo de triaje); los elementos con condición no
 * resuelta se omiten (nunca se inserta un factor huérfano).</p>
 */
public class DifferentialCatalogParser {

    private final ObjectMapper objectMapper;

    public DifferentialCatalogParser() {
        this(new ObjectMapper());
    }

    public DifferentialCatalogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * @param content            JSON estructurado del dataset
     * @param conditionResolver  resuelve nombre canónico de condición → id
     * @return actualización de dominio (puede estar vacía)
     */
    public CatalogUpdate parse(String content, Function<String, Optional<UUID>> conditionResolver) {
        if (content == null || content.isBlank() || conditionResolver == null) {
            return CatalogUpdate.empty();
        }
        try {
            DifferentialCatalogEntry entry = objectMapper.readValue(content, DifferentialCatalogEntry.class);
            return toUpdate(entry, conditionResolver);
        } catch (JsonProcessingException | RuntimeException ex) {
            return CatalogUpdate.empty();
        }
    }

    private CatalogUpdate toUpdate(DifferentialCatalogEntry entry, Function<String, Optional<UUID>> conditionResolver) {
        if (entry == null) {
            return CatalogUpdate.empty();
        }
        List<RiskFactor> risks = new ArrayList<>();
        List<RecommendedTest> tests = new ArrayList<>();
        if (entry.getRiskFactors() != null) {
            for (DifferentialCatalogEntry.RiskFactorRef ref : entry.getRiskFactors()) {
                if (ref == null || ref.getFactor() == null || ref.getFactor().isBlank()) {
                    continue;
                }
                conditionResolver
                        .apply(ref.getCondition())
                        .ifPresent(conditionId -> risks.add(RiskFactor.of(
                                DeterministicId.from("risk-factor", conditionId.toString(), ref.getFactor()),
                                conditionId,
                                ref.getFactor(),
                                ref.getWeight(),
                                ref.getDescription())));
            }
        }
        if (entry.getRecommendedTests() != null) {
            for (DifferentialCatalogEntry.TestRef ref : entry.getRecommendedTests()) {
                if (ref == null || ref.getTest() == null || ref.getTest().isBlank()) {
                    continue;
                }
                conditionResolver
                        .apply(ref.getCondition())
                        .ifPresent(conditionId -> tests.add(RecommendedTest.of(
                                DeterministicId.from("recommended-test", conditionId.toString(), ref.getTest()),
                                conditionId,
                                ref.getTest(),
                                ref.getDescription())));
            }
        }
        return CatalogUpdate.of("health-differential", List.copyOf(risks), List.copyOf(tests));
    }
}
