package com.kinplatform.kin.health.triage.stage;

import com.kinplatform.kin.health.triage.domain.SymptomExtractor;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageInput;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.kin.health.triage.event.TriagePerformedEvent;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.common.pipeline.PipelineContext;
import com.kinplatform.common.pipeline.PipelineStage;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Etapa de triaje del pipeline (ADR-028).
 *
 * <p>Composición pura sobre {@link TriageEngine}: extrae los síntomas del
 * mensaje del usuario con un {@link SymptomExtractor} (keywords determinista o
 * NLP con fallback) y, solo si hay síntomas reconocidos, ejecuta el motor y
 * almacena el {@link TriageResult} en {@code PipelineContext.triageResult}
 * (campo aditivo sancionado por ADR-028). Emite {@link TriagePerformedEvent}
 * para logging y métricas.</p>
 *
 * <p><strong>Modo seguro</strong>: si el módulo está deshabilitado
 * ({@code kin.health.triage.enabled=false}) o no hay síntomas en el mensaje,
 * la etapa se omite (no modifica el contexto ni el flujo existente).</p>
 */
public class TriageStage implements PipelineStage {

    private static final Logger log = LoggerFactory.getLogger(TriageStage.class);

    private final TriageEngine engine;
    private final TriageKnowledgeRepository knowledgeRepository;
    private final SymptomExtractor symptomExtractor;
    private final boolean enabled;

    public TriageStage(TriageEngine engine, TriageKnowledgeRepository knowledgeRepository, boolean enabled) {
        this(
                engine,
                knowledgeRepository,
                new com.kinplatform.kin.health.triage.domain.KeywordSymptomExtractor(),
                enabled);
    }

    public TriageStage(
            TriageEngine engine,
            TriageKnowledgeRepository knowledgeRepository,
            SymptomExtractor symptomExtractor,
            boolean enabled) {
        this.engine = engine;
        this.knowledgeRepository = knowledgeRepository;
        this.symptomExtractor = symptomExtractor;
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "Triaje";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return enabled
                && context != null
                && context.projectContext() != null
                && context.userMessage() != null
                && !context.userMessage().isBlank()
                && isHealthCategory(context.projectCategory());
    }

    /**
     * Gate por categoría: el triaje solo aplica a proyectos de salud
     * (código {@code SALUD} del catálogo de categorías). Evita cargar el
     * catálogo de triaje en cada turno de proyectos no sanitarios.
     */
    private static boolean isHealthCategory(String projectCategory) {
        return projectCategory != null && projectCategory.trim().equalsIgnoreCase("SALUD");
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        if (!enabled || context == null || !isHealthCategory(context.projectCategory())) {
            return context;
        }
        TriageCatalog catalog = knowledgeRepository.loadCatalog();
        List<String> symptoms = symptomExtractor.extract(catalog, context.userMessage());
        if (symptoms.isEmpty()) {
            log.debug("TriageStage: sin síntomas reconocidos en el mensaje, se omite");
            return context;
        }
        TriageResult result = engine.evaluate(TriageInput.of(symptoms), catalog);
        if (result.isEmpty()) {
            log.debug("TriageStage: sin condiciones candidatas para {}", symptoms);
            return context;
        }
        context.triageResult(result);
        context.setEngineResult(engine.metadata().name(), result);
        Urgency maxUrgency = result.results().stream()
                .map(com.kinplatform.kin.health.triage.domain.TriageConditionResult::urgency)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(Urgency.BAJA);
        context.addEvent(new TriagePerformedEvent(
                context.userId(),
                context.projectId(),
                symptoms,
                result.results().size(),
                maxUrgency,
                result.results().stream()
                        .map(com.kinplatform.kin.health.triage.domain.TriageConditionResult::name)
                        .toList()));
        log.info(
                "TriageStage: {} síntomas -> {} condiciones candidatas (top={}, urgencia máxima={})",
                symptoms.size(),
                result.results().size(),
                result.results().isEmpty() ? "-" : result.results().get(0).name(),
                maxUrgency);
        return context;
    }
}

