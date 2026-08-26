package com.kinplatform.kin.health.differential.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;
import com.kinplatform.kin.health.differential.domain.DifferentialInput;
import com.kinplatform.kin.health.differential.domain.DifferentialItem;
import com.kinplatform.kin.health.differential.domain.DifferentialResult;
import com.kinplatform.kin.health.differential.domain.PatientContext;
import com.kinplatform.kin.health.differential.domain.RecommendedTest;
import com.kinplatform.kin.health.differential.domain.RiskFactor;
import com.kinplatform.kin.health.differential.port.DifferentialKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor determinista de diagnóstico diferencial (ADR-029).
 *
 * <p>Parte del {@code TriageResult} (base del triaje) y lo enriquece con
 * conocimiento clínico estructurado. Reglas 100 % en Java, sin LLM:</p>
 *
 * <ol>
 *   <li>Cada condición candidata del triaje se convierte en un
 *       {@link DifferentialItem} conservando probabilidad, severidad, urgencia
 *       y síntomas coincidentes.</li>
 *   <li>Se calcula el ajuste por factores de riesgo: para cada
 *       {@link RiskFactor} del catálogo cuya clave coincida con el
 *       {@link PatientContext}, la probabilidad se modifica con
 *       {@code p' = p + weight·(1-p)} (siempre creciente y acotada a [0,1]).</li>
 *   <li>Se recomiendan pruebas complementarias del catálogo para las
 *       condiciones con mayor probabilidad (las que ayudan a diferenciar).</li>
 *   <li>Se genera un {@code reasoning} con template determinista en Java.</li>
 *   <li>Se ordena por probabilidad descendente y se limita el resultado.</li>
 * </ol>
 *
 * <p>Nunca persiste, no habla con la red y nunca genera prompts. Usa el puerto
 * {@link DifferentialKnowledgeRepository} para cargar el catálogo.</p>
 */
public class DifferentialEngine implements DomainEngine<DifferentialInput, DifferentialResult> {

    public static final String GENERATOR_NAME = "DifferentialEngine";
    public static final String ENGINE_VERSION = "v1";

    private final DifferentialKnowledgeRepository knowledgeRepository;
    private final int maxItems;

    public DifferentialEngine(DifferentialKnowledgeRepository knowledgeRepository) {
        this(knowledgeRepository, 5);
    }

    public DifferentialEngine(DifferentialKnowledgeRepository knowledgeRepository, int maxItems) {
        if (knowledgeRepository == null) {
            throw new IllegalArgumentException("knowledgeRepository no puede ser null");
        }
        this.knowledgeRepository = knowledgeRepository;
        this.maxItems = Math.max(1, maxItems);
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(
                GENERATOR_NAME, ENGINE_VERSION, "KIN Architecture Team", EnginePhase.VALIDATION, EngineType.DOMAIN, 56);
    }

    @Override
    public DifferentialResult evaluate(DifferentialInput input) {
        if (input == null || input.isEmpty()) {
            return DifferentialResult.empty();
        }
        return evaluate(input, knowledgeRepository.loadCatalog());
    }

    /**
     * Evaluación pura contra un catálogo dado (determinista y testeable).
     */
    public DifferentialResult evaluate(DifferentialInput input, DifferentialCatalog catalog) {
        if (input == null || input.isEmpty() || catalog == null) {
            return DifferentialResult.empty();
        }
        List<DifferentialItem> items = new ArrayList<>();
        for (TriageConditionResult base : input.triageConditions()) {
            DifferentialItem item = buildItem(base, input.patientContext(), catalog);
            if (item != null) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return DifferentialResult.empty();
        }
        items.sort(Comparator.comparingDouble(DifferentialItem::probability).reversed());
        List<DifferentialItem> limited = items.size() > maxItems ? items.subList(0, maxItems) : items;
        double confidence = limited.get(0).probability();
        String explanation = "Diagnóstico diferencial con " + limited.size() + " condición(es) " + "priorizada(s) y "
                + totalTests(limited) + " prueba(s) sugerida(s).";
        return new DifferentialResult(
                List.copyOf(limited), input.symptoms(), confidence, explanation, GENERATOR_NAME, ENGINE_VERSION);
    }

    private DifferentialItem buildItem(
            TriageConditionResult base, PatientContext patientContext, DifferentialCatalog catalog) {
        List<RiskFactor> matched = new ArrayList<>();
        for (RiskFactor riskFactor : catalog.riskFactorsFor(base.conditionId())) {
            if (patientContext.has(riskFactor.factor())) {
                matched.add(riskFactor);
            }
        }
        double adjusted = adjust(base.probability(), matched);
        List<RecommendedTest> tests = catalog.recommendedTestsFor(base.conditionId());
        return new DifferentialItem(
                base.conditionId(),
                base.name(),
                base.description(),
                adjusted,
                base.severity(),
                base.urgency(),
                base.matchedSymptoms(),
                List.copyOf(matched),
                tests,
                reasoning(base, adjusted, matched));
    }

    /**
     * Ajuste determinista: p' = p + Σ weight·(1-p), acotado a [0,1].
     */
    private double adjust(double probability, List<RiskFactor> matched) {
        double adjusted = probability;
        for (RiskFactor riskFactor : matched) {
            adjusted = adjusted + riskFactor.weight() * (1.0 - adjusted);
        }
        return Math.max(0.0, Math.min(1.0, Math.round(adjusted * 10_000.0) / 10_000.0));
    }

    private String reasoning(TriageConditionResult base, double adjusted, List<RiskFactor> matched) {
        StringBuilder sb = new StringBuilder();
        sb.append(base.name())
                .append(" se sugiere por la coincidencia de ")
                .append(base.matchedSymptoms().size())
                .append(" síntoma(s): ")
                .append(String.join(", ", base.matchedSymptoms()))
                .append(".");
        if (!matched.isEmpty()) {
            sb.append(" Riesgo aumentado por: ")
                    .append(String.join(
                            ", ", matched.stream().map(RiskFactor::factor).toList()))
                    .append(".");
        }
        return sb.toString();
    }

    private int totalTests(List<DifferentialItem> items) {
        int total = 0;
        for (DifferentialItem item : items) {
            total += item.recommendedTests().size();
        }
        return total;
    }
}
