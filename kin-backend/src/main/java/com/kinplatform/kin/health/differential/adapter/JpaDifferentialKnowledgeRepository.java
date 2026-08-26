package com.kinplatform.kin.health.differential.adapter;

import com.kinplatform.kin.engine.DeterministicId;
import com.kinplatform.kin.health.differential.domain.CatalogUpdate;
import com.kinplatform.kin.health.differential.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.differential.port.DifferentialKnowledgeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link DifferentialKnowledgeRepository} (ADR-029).
 *
 * <p>Carga el catálogo de factores de riesgo y pruebas recomendadas desde las
 * tablas {@code risk_factors} y {@code recommended_tests}, y aplica
 * actualizaciones masivas como upsert idempotente con IDs deterministas.</p>
 */
@Component
public class JpaDifferentialKnowledgeRepository implements DifferentialKnowledgeRepository {

    private final RiskFactorJpaRepository riskFactorRepository;
    private final RecommendedTestJpaRepository testRepository;

    public JpaDifferentialKnowledgeRepository(
            RiskFactorJpaRepository riskFactorRepository, RecommendedTestJpaRepository testRepository) {
        this.riskFactorRepository = riskFactorRepository;
        this.testRepository = testRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialCatalog loadCatalog() {
        var risks = riskFactorRepository.findAll().stream()
                .map(e -> RiskFactor.of(
                        e.getId(),
                        e.getConditionId(),
                        e.getFactor(),
                        e.getWeight() == null ? 0.0 : e.getWeight(),
                        e.getDescription()))
                .toList();
        var tests = testRepository.findAll().stream()
                .map(e -> RecommendedTest.of(e.getId(), e.getConditionId(), e.getTest(), e.getDescription()))
                .toList();
        return new DifferentialCatalog(risks, tests);
    }

    @Override
    @Transactional
    public CatalogUpdateResult applyUpdate(CatalogUpdate update) {
        if (update == null || update.isEmpty()) {
            return CatalogUpdateResult.empty(update == null ? "" : update.source());
        }
        int risksAdded = upsertRiskFactors(update.riskFactors());
        int testsAdded = upsertTests(update.recommendedTests());
        return CatalogUpdateResult.of(risksAdded, testsAdded, update.source());
    }

    private int upsertRiskFactors(List<RiskFactor> riskFactors) {
        int added = 0;
        for (RiskFactor riskFactor : riskFactors) {
            if (riskFactor == null || riskFactor.conditionId() == null) {
                continue;
            }
            UUID id = riskFactor.id() != null
                    ? riskFactor.id()
                    : DeterministicId.from(
                            "risk-factor", riskFactor.conditionId().toString(), riskFactor.factor());
            RiskFactorEntity entity = riskFactorRepository.findById(id).orElse(null);
            if (entity == null) {
                entity = new RiskFactorEntity();
                entity.setId(id);
                entity.setCreatedAt(java.time.OffsetDateTime.now());
                added++;
            }
            entity.setConditionId(riskFactor.conditionId());
            entity.setFactor(riskFactor.factor());
            entity.setWeight(riskFactor.weight());
            entity.setDescription(riskFactor.description());
            riskFactorRepository.save(entity);
        }
        return added;
    }

    private int upsertTests(List<RecommendedTest> tests) {
        int added = 0;
        for (RecommendedTest test : tests) {
            if (test == null || test.conditionId() == null) {
                continue;
            }
            UUID id = test.id() != null
                    ? test.id()
                    : DeterministicId.from(
                            "recommended-test", test.conditionId().toString(), test.test());
            RecommendedTestEntity entity = testRepository.findById(id).orElse(null);
            if (entity == null) {
                entity = new RecommendedTestEntity();
                entity.setId(id);
                entity.setCreatedAt(java.time.OffsetDateTime.now());
                added++;
            }
            entity.setConditionId(test.conditionId());
            entity.setTest(test.test());
            entity.setDescription(test.description());
            testRepository.save(entity);
        }
        return added;
    }
}
