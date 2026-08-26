package com.kinplatform.kin.health.differential.stage;

import com.kinplatform.kin.health.differential.domain.DifferentialInput;
import com.kinplatform.kin.health.differential.domain.DifferentialResult;
import com.kinplatform.kin.health.differential.engine.DifferentialEngine;
import com.kinplatform.kin.health.differential.event.DifferentialPerformedEvent;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Etapa de diagnóstico diferencial del pipeline (ADR-029).
 *
 * <p>Composición pura sobre {@link DifferentialEngine}: lee el
 * {@code PipelineContext.triageResult} producido por la {@code TriageStage} y,
 * si hay condiciones candidatas, ejecuta el motor y almacena el
 * {@link DifferentialResult} en {@code PipelineContext.differentialResult}
 * (campo aditivo). Emite {@link DifferentialPerformedEvent} para logging y
 * métricas.</p>
 *
 * <p><strong>Modo seguro</strong>: si el módulo está deshabilitado
 * ({@code kin.health.differential.enabled=false}) o no hay resultado de triaje,
 * la etapa se omite sin modificar el flujo existente.</p>
 */
public class DifferentialStage implements PipelineStage {

    private static final Logger log = LoggerFactory.getLogger(DifferentialStage.class);

    private final DifferentialEngine engine;
    private final boolean enabled;

    public DifferentialStage(DifferentialEngine engine, boolean enabled) {
        this.engine = engine;
        this.enabled = enabled;
    }

    @Override
    public String name() {
        return "Diagnóstico diferencial";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return enabled
                && context != null
                && context.projectContext() != null
                && context.triageResult() != null
                && !context.triageResult().isEmpty();
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        if (!enabled || context == null) {
            return context;
        }
        TriageResult triage = context.triageResult();
        if (triage == null || triage.isEmpty()) {
            return context;
        }
        DifferentialInput input = DifferentialInput.of(
                triage.unrecognizedSymptoms(),
                triage.results().stream()
                        .map(r -> new com.kinplatform.kin.health.triage.domain.TriageConditionResult(
                                r.conditionId(),
                                r.name(),
                                r.description(),
                                r.probability(),
                                r.severity(),
                                r.urgency(),
                                r.recommendation(),
                                r.matchedSymptoms()))
                        .toList());
        DifferentialResult result = engine.evaluate(input);
        if (result.isEmpty()) {
            log.debug("DifferentialStage: sin condiciones para diagnóstico diferencial");
            return context;
        }
        context.differentialResult(result);
        context.setEngineResult(engine.metadata().name(), result);
        context.addEvent(new DifferentialPerformedEvent(
                context.userId(),
                context.projectId(),
                triage.unrecognizedSymptoms(),
                result.items().size()));
        log.info(
                "DifferentialStage: {} condiciones priorizadas (top={})",
                result.items().size(),
                result.items().isEmpty() ? "-" : result.items().get(0).name());
        return context;
    }
}
