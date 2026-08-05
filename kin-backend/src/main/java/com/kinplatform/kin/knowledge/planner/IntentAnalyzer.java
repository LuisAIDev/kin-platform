package com.kinplatform.kin.knowledge.planner;

import com.kinplatform.kin.knowledge.KnowledgeRequest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Primer paso del pipeline del Query Planner (especificación Fase 3): identifica
 * de forma determinista las intenciones del usuario a partir del
 * {@link KnowledgeRequest}, sin IA ni embeddings — solo reglas declarativas
 * registrables.
 *
 * <p>Detecta conocimiento estable (p. ej. "scrum"), facetas directas por reglas
 * y facetas implicadas (p. ej. MERCADO implica ESTADISTICA), y deriva el tipo
 * primario con el {@link FacetOrder}.</p>
 */
public class IntentAnalyzer {

    private final List<IntentRule> rules;
    private final FacetImplication implications;
    private final FacetOrder order;

    public IntentAnalyzer() {
        this(defaultRules(), FacetImplication.defaults(), FacetOrder.defaults());
    }

    public IntentAnalyzer(List<IntentRule> rules, FacetImplication implications, FacetOrder order) {
        this.rules = rules == null ? defaultRules() : List.copyOf(rules);
        this.implications = implications == null ? FacetImplication.defaults() : implications;
        this.order = order == null ? FacetOrder.defaults() : order;
    }

    public QueryIntent analyze(KnowledgeRequest request) {
        if (request == null || isEmptyRequest(request)) {
            return QueryIntent.general();
        }
        String text = KeywordIntentRule.normalize(
            request.topic() + " " + String.join(" ", request.keywords()));
        boolean stable = rules.stream().anyMatch(rule -> rule.stable() && rule.matches(text));
        if (stable) {
            return new QueryIntent(IntentType.CONOCIMIENTO_ESTABLE, Set.of(), request.topic());
        }
        var facets = new LinkedHashSet<IntentFacet>();
        for (var rule : rules) {
            if (!rule.stable() && rule.matches(text)) {
                facets.add(rule.facet());
            }
        }
        var withImplied = new LinkedHashSet<>(facets);
        for (var facet : facets) {
            withImplied.addAll(implications.impliedBy(facet));
        }
        if (withImplied.isEmpty()) {
            return new QueryIntent(IntentType.GENERAL, Set.of(), request.topic());
        }
        IntentFacet primary = order.primaryOf(withImplied).orElseGet(() -> withImplied.iterator().next());
        return new QueryIntent(IntentType.valueOf(primary.name()), Set.copyOf(withImplied), request.topic());
    }

    /**
     * Reglas de intención por defecto (declarativas y registrables). Texto
     * normalizado sin acentos y en minúsculas.
     */
    public static List<IntentRule> defaultRules() {
        return List.of(
            KeywordIntentRule.of(IntentFacet.REGULATORIA,
                "sas", "constituir", "formalizar", "legalizar", "permiso", "permisos",
                "licencia", "tramite", "tramites", "requisito", "requisitos", "registro",
                "regulacion", "normativa", "abrir"),
            KeywordIntentRule.of(IntentFacet.LEGAL,
                "legal", "ley", "leyes", "norma", "normas", "contrato", "contratos",
                "juridico", "abogado", "sas"),
            KeywordIntentRule.of(IntentFacet.FINANCIERA,
                "financiero", "financiera", "inversion", "capital", "costo", "costos",
                "presupuesto", "rentabilidad", "impuesto", "impuestos", "tributo",
                "tributaria", "tributarios"),
            KeywordIntentRule.of(IntentFacet.ACADEMICA,
                "academico", "tesis", "investigacion", "universidad", "estudio", "paper",
                "articulo", "bibliografia"),
            KeywordIntentRule.of(IntentFacet.TECNICA,
                "tecnico", "tecnica", "implementacion", "arquitectura", "software",
                "algoritmo", "proceso", "prototipo"),
            KeywordIntentRule.of(IntentFacet.MERCADO,
                "mercado", "sector", "demanda", "clientes", "consumo", "panorama",
                "tamano", "negocio", "emprender", "panaderia"),
            KeywordIntentRule.of(IntentFacet.ESTADISTICA,
                "estadistica", "estadisticas", "datos", "cifras", "indicador",
                "indicadores", "estadistico"),
            KeywordIntentRule.of(IntentFacet.COMPETENCIA,
                "competencia", "competidor", "competidores", "rival", "rivales", "benchmark"),
            KeywordIntentRule.of(IntentFacet.TENDENCIAS,
                "tendencia", "tendencias", "novedades", "proyeccion", "proyecciones",
                "crecimiento"),
            KeywordIntentRule.of(IntentFacet.DOCUMENTO,
                "pdf", "documento", "archivo", "adjunto", "analiza", "analizar", "leer",
                "word", "excel", "subir"),
            KeywordIntentRule.of(IntentFacet.RAG,
                "rag", "base interna", "conocimiento interno", "vector", "memoria interna"),
            KeywordIntentRule.stable("ConocimientoEstable",
                "scrum", "kanban", "metodologia agil", "metodologia scrum"));
    }

    private static boolean isEmptyRequest(KnowledgeRequest request) {
        return (request.topic() == null || request.topic().isBlank()) && request.keywords().isEmpty();
    }
}
