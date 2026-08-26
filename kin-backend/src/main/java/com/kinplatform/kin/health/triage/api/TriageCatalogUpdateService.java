package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.adapter.HealthCatalogParser;
import com.kinplatform.kin.health.triage.adapter.HealthKnowledgeAdapter;
import com.kinplatform.kin.health.triage.config.TriageProperties;
import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeInput;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de actualización del catálogo de triaje desde fuentes externas
 * (ADR-028, fase profesional).
 *
 * <p>Integra el {@link KnowledgeEngine} existente: primero intenta adquirir
 * conocimiento médico a través del motor (fuentes externas configuradas en la
 * capa de conocimiento). Si el motor no produce datos estructurados (offline,
 * sin fuentes, red caída), degrada con gracia al {@link HealthKnowledgeAdapter}
 * (dataset empaquetado CIE-10/WHO) y aplica la actualización como upsert
 * idempotente. Nunca rompe el catálogo local: ante cualquier fallo devuelve un
 * resultado vacío y conserva los datos existentes.</p>
 */
@Service
public class TriageCatalogUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TriageCatalogUpdateService.class);

    private final KnowledgeEngine knowledgeEngine;
    private final HealthKnowledgeAdapter healthAdapter;
    private final TriageKnowledgeRepository knowledgeRepository;
    private final HealthCatalogParser parser;
    private final TriageProperties properties;

    public TriageCatalogUpdateService(
            KnowledgeEngine knowledgeEngine,
            HealthKnowledgeAdapter healthAdapter,
            TriageKnowledgeRepository knowledgeRepository,
            HealthCatalogParser parser,
            TriageProperties properties) {
        this.knowledgeEngine = knowledgeEngine;
        this.healthAdapter = healthAdapter;
        this.knowledgeRepository = knowledgeRepository;
        this.parser = parser;
        this.properties = properties;
    }

    /**
     * Fuerza la actualización del catálogo desde las fuentes externas. Devuelve
     * el conteo de elementos añadidos/actualizados; ante fallo devuelve un
     * resultado vacío (catálogo local intacto).
     */
    @Transactional
    public CatalogUpdateResult updateCatalog() {
        if (!properties.isEnabled()) {
            return CatalogUpdateResult.empty("Módulo de triaje deshabilitado");
        }
        KnowledgeRequest request = catalogRequest();
        try {
            CatalogUpdate fromEngine = acquireViaEngine(request);
            if (!fromEngine.isEmpty()) {
                return knowledgeRepository.applyUpdate(fromEngine);
            }
        } catch (RuntimeException ex) {
            log.warn(
                    "TriageCatalogUpdateService: motor de conocimiento falló, degrada al adapter ({})",
                    ex.getMessage());
        }
        try {
            CatalogUpdate fromAdapter = acquireViaAdapter(request);
            if (!fromAdapter.isEmpty()) {
                return knowledgeRepository.applyUpdate(fromAdapter);
            }
            log.info("TriageCatalogUpdateService: sin datos de fuente externa, catálogo local intacto");
            return CatalogUpdateResult.empty("Sin datos de fuente externa");
        } catch (RuntimeException ex) {
            log.warn(
                    "TriageCatalogUpdateService: fallo de actualización, catálogo local intacto ({})", ex.getMessage());
            return CatalogUpdateResult.empty("Fallo de fuente externa; catálogo local intacto");
        }
    }

    private KnowledgeRequest catalogRequest() {
        return new KnowledgeRequest(
                "catálogo de condiciones médicas y síntomas",
                java.util.Set.of(),
                List.of("salud", "CIE-10", "condiciones", "síntomas", "triaje"),
                KnowledgeRequest.MAX_LIMIT,
                Duration.ofDays(365),
                "SALUD");
    }

    /**
     * Intenta adquirir el catálogo a través del {@link KnowledgeEngine}. Sin
     * fuentes externas configuradas el motor degrada offline a vacío.
     */
    private CatalogUpdate acquireViaEngine(KnowledgeRequest request) {
        KnowledgeResult result = knowledgeEngine.evaluate(KnowledgeInput.of(request));
        if (result == null || result.isEmpty()) {
            return CatalogUpdate.empty();
        }
        return parser.parseFacts(result.facts());
    }

    /**
     * Fallback directo al {@link HealthKnowledgeAdapter} (dataset empaquetado o
     * API médica configurada). El adaptador es SSRF-safe y offline-first.
     */
    private CatalogUpdate acquireViaAdapter(KnowledgeRequest request) {
        var candidates = healthAdapter.fetch(KnowledgeQuery.from(request));
        return parser.parseCandidates(candidates);
    }
}
