package com.kinplatform.kin.health.differential.api;

import com.kinplatform.kin.health.differential.adapter.DifferentialCatalogParser;
import com.kinplatform.kin.health.differential.adapter.DifferentialKnowledgeAdapter;
import com.kinplatform.kin.health.differential.config.DifferentialProperties;
import com.kinplatform.kin.health.differential.domain.CatalogUpdate;
import com.kinplatform.kin.health.differential.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.differential.port.DifferentialKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de actualización del catálogo del diagnóstico diferencial desde
 * fuentes externas (ADR-029).
 *
 * <p>Integra el {@link KnowledgeEngine} existente: primero intenta adquirir el
 * conocimiento médico a través del motor; si no produce datos estructurados,
 * degrada con gracia al {@link DifferentialKnowledgeAdapter} (bundle
 * empaquetado) y aplica la actualización como upsert idempotente. Los nombres de
 * condición se resuelven contra el catálogo de triaje; nunca se inserta un
 * factor/prueba huérfano. Ante fallo total, el catálogo local permanece
 * intacto.</p>
 */
@Service
public class DifferentialCatalogUpdateService {

    private static final Logger log = LoggerFactory.getLogger(DifferentialCatalogUpdateService.class);

    private final KnowledgeEngine knowledgeEngine;
    private final DifferentialKnowledgeAdapter adapter;
    private final DifferentialKnowledgeRepository knowledgeRepository;
    private final TriageKnowledgeRepository triageKnowledgeRepository;
    private final DifferentialCatalogParser parser;
    private final DifferentialProperties properties;

    public DifferentialCatalogUpdateService(
            KnowledgeEngine knowledgeEngine,
            DifferentialKnowledgeAdapter adapter,
            DifferentialKnowledgeRepository knowledgeRepository,
            TriageKnowledgeRepository triageKnowledgeRepository,
            DifferentialCatalogParser parser,
            DifferentialProperties properties) {
        this.knowledgeEngine = knowledgeEngine;
        this.adapter = adapter;
        this.knowledgeRepository = knowledgeRepository;
        this.triageKnowledgeRepository = triageKnowledgeRepository;
        this.parser = parser;
        this.properties = properties;
    }

    @Transactional
    public CatalogUpdateResult updateCatalog() {
        if (!properties.isEnabled()) {
            return CatalogUpdateResult.empty("Módulo de diagnóstico diferencial deshabilitado");
        }
        TriageCatalog triageCatalog = triageKnowledgeRepository.loadCatalog();
        KnowledgeRequest request = catalogRequest();
        try {
            KnowledgeResult result = knowledgeEngine.evaluate(KnowledgeInput.of(request));
            if (result != null && !result.isEmpty()) {
                CatalogUpdate fromEngine = parseFacts(result, triageCatalog);
                if (!fromEngine.isEmpty()) {
                    return knowledgeRepository.applyUpdate(fromEngine);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("DifferentialCatalogUpdateService: motor falló, degrada al adapter ({})", ex.getMessage());
        }
        try {
            var candidates = adapter.fetch(KnowledgeQuery.from(request));
            if (!candidates.isEmpty()) {
                CatalogUpdate fromAdapter = parser.parse(candidates.get(0).content(), resolver(triageCatalog));
                if (!fromAdapter.isEmpty()) {
                    return knowledgeRepository.applyUpdate(fromAdapter);
                }
            }
            log.info("DifferentialCatalogUpdateService: sin datos de fuente externa, catálogo local intacto");
            return CatalogUpdateResult.empty("Sin datos de fuente externa");
        } catch (RuntimeException ex) {
            log.warn(
                    "DifferentialCatalogUpdateService: fallo de actualización, catálogo local intacto ({})",
                    ex.getMessage());
            return CatalogUpdateResult.empty("Fallo de fuente externa; catálogo local intacto");
        }
    }

    private KnowledgeRequest catalogRequest() {
        return new KnowledgeRequest(
                "factores de riesgo y pruebas de diagnóstico diferencial",
                java.util.Set.of(),
                List.of("salud", "factores de riesgo", "pruebas", "diagnóstico diferencial"),
                KnowledgeRequest.MAX_LIMIT,
                Duration.ofDays(365),
                "SALUD");
    }

    private CatalogUpdate parseFacts(KnowledgeResult result, TriageCatalog triageCatalog) {
        CatalogUpdate acc = CatalogUpdate.empty();
        for (var fact : result.facts()) {
            CatalogUpdate parsed = parser.parse(fact.claim(), resolver(triageCatalog));
            if (!parsed.isEmpty()) {
                acc = merge(acc, parsed);
            }
        }
        return acc;
    }

    private CatalogUpdate merge(CatalogUpdate a, CatalogUpdate b) {
        var risks = new java.util.ArrayList<>(a.riskFactors());
        risks.addAll(b.riskFactors());
        var tests = new java.util.ArrayList<>(a.recommendedTests());
        tests.addAll(b.recommendedTests());
        return CatalogUpdate.of("health-differential", List.copyOf(risks), List.copyOf(tests));
    }

    private java.util.function.Function<String, Optional<UUID>> resolver(TriageCatalog triageCatalog) {
        return name -> triageCatalog.conditions().stream()
                .filter(c -> c.name().equalsIgnoreCase(name == null ? "" : name.strip()))
                .map(c -> Optional.<UUID>of(c.id()))
                .findFirst()
                .orElse(Optional.empty());
    }
}
