package com.kinplatform.kin.health.triage.adapter;

import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.common.knowledge.KnowledgeInput;
import com.kinplatform.common.knowledge.KnowledgeQuery;
import com.kinplatform.common.knowledge.KnowledgeRequest;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importador de datos clínicos desde fuentes externas (ADR-028, fase de
 * consolidación).
 *
 * <p>Integra el {@link KnowledgeEngine} existente para adquirir condiciones y
 * síntomas desde APIs médicas abiertas (WHO, PubMed, datasets estructurados) o
 * desde el bundle empaquetado ({@link HealthKnowledgeAdapter}), los normaliza y
 * deduplica mediante IDs deterministas y aplica la actualización al repositorio
 * local con estado {@code PENDING} (debe pasar por validación clínica antes de
 * usarse en producción).</p>
 *
 * <p>Degradación elegante: si el motor o la fuente externa fallan, se usa el
 * bundle empaquetado; ante fallo total, el catálogo local permanece intacto.</p>
 */
@Service
public class HealthDataImporter {

    private static final Logger log = LoggerFactory.getLogger(HealthDataImporter.class);

    private final KnowledgeEngine knowledgeEngine;
    private final HealthKnowledgeAdapter healthAdapter;
    private final HealthCatalogParser parser;
    private final TriageKnowledgeRepository knowledgeRepository;

    public HealthDataImporter(
            KnowledgeEngine knowledgeEngine,
            HealthKnowledgeAdapter healthAdapter,
            HealthCatalogParser parser,
            TriageKnowledgeRepository knowledgeRepository) {
        this.knowledgeEngine = knowledgeEngine;
        this.healthAdapter = healthAdapter;
        this.parser = parser;
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * Importa condiciones/síntomas desde fuentes externas y aplica la
     * actualización al catálogo local (upsert idempotente con estado PENDING).
     *
     * @return resultado de la importación
     */
    @Transactional
    public CatalogUpdateResult importFromExternal() {
        KnowledgeRequest request = catalogRequest();
        try {
            KnowledgeResult result = knowledgeEngine.evaluate(KnowledgeInput.of(request));
            if (result != null && !result.isEmpty()) {
                CatalogUpdate fromEngine = parser.parseFacts(result.facts());
                if (!fromEngine.isEmpty()) {
                    return knowledgeRepository.applyUpdate(fromEngine);
                }
            }
            log.info("HealthDataImporter: KnowledgeEngine sin datos estructurados, degrada al bundle");
            return importFromBundle();
        } catch (RuntimeException ex) {
            log.warn("HealthDataImporter: motor falló, degrada al bundle ({})", ex.getMessage());
            return importFromBundle();
        }
    }

    private CatalogUpdateResult importFromBundle() {
        var candidates = healthAdapter.fetch(KnowledgeQuery.from(catalogRequest()));
        CatalogUpdate update = parser.parseCandidates(candidates);
        if (update.isEmpty()) {
            log.info("HealthDataImporter: bundle sin datos, catálogo local intacto");
            return CatalogUpdateResult.empty("Sin datos de fuente externa");
        }
        return knowledgeRepository.applyUpdate(update);
    }

    private KnowledgeRequest catalogRequest() {
        return new KnowledgeRequest(
                "catálogo de condiciones médicas y síntomas CIE-10",
                java.util.Set.of(),
                List.of("salud", "CIE-10", "condiciones", "síntomas", "triaje"),
                KnowledgeRequest.MAX_LIMIT,
                Duration.ofDays(365),
                "SALUD");
    }
}

