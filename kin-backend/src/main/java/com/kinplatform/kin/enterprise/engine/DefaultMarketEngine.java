package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.enterprise.engine.input.MarketInput;
import com.kinplatform.kin.enterprise.engine.result.MarketResult;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.knowledge.KnowledgeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementación determinista del {@link MarketEngine} (Fase 10, Milestone 2D).
 *
 * <p>Construye el {@link MarketPlan} priorizando los hechos verificados del
 * {@link KnowledgeResult} y operando en modo offline cuando el resultado de
 * conocimiento está vacío. Reglas funcionales aplicadas:</p>
 *
 * <ul>
 *   <li>TAM, SAM y SOM se extraen de forma determinista de las afirmaciones
 *       numéricas del conocimiento externo (el primer valor numérico de cada
 *       hecho se asigna a TAM, el siguiente a SAM y el siguiente a SOM). Sin
 *       datos numéricos, quedan en cero (modo offline).</li>
 *   <li>{@code growthRate} se toma del primer valor numérico de una
 *       afirmación que mencione crecimiento (palabras clave o {@code %});
 *       si no existe, del primer valor numérico disponible.</li>
 *   <li>{@code customerSegments} &larr; contexto {@code TARGET_CUSTOMER}.</li>
 *   <li>{@code competitors} &larr; contexto {@code COMPETITION} combinado con
 *       los hechos del conocimiento cuando existen.</li>
 *   <li>{@code channels} y {@code entryBarriers} se alimentan de los hechos
 *       del conocimiento; si no hay conocimiento, quedan en
 *       {@code "Por definir"}.</li>
 *   <li>La confianza es la del conocimiento externo; en modo offline es 0.0.</li>
 * </ul>
 *
 * <p>Motor stateless, thread-safe, sin dependencias, sin Spring y sin efectos
 * secundarios.</p>
 */
public class DefaultMarketEngine implements MarketEngine {

    /** Marcador de datos ausentes (regla funcional: nunca inventar). */
    static final String UNDEFINED = "Por definir";

    private static final String ENGINE_NAME = "kin.enterprise:Market";
    private static final String ENGINE_VERSION = "1.0.0";
    private static final String ENGINE_AUTHOR = "KIN Architecture Team";
    private static final int ENGINE_PRIORITY = 81;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(ENGINE_NAME, ENGINE_VERSION, ENGINE_AUTHOR,
            EnginePhase.MARKET, EngineType.DOMAIN, ENGINE_PRIORITY);
    }

    @Override
    public MarketResult evaluate(MarketInput input) {
        if (input == null || input.context() == null || !input.context().hasKnownDimensions()) {
            return MarketResult.empty();
        }
        var context = input.context();
        var knowledge = input.knowledge();

        var sizes = extractSizes(knowledge);
        double tam = sizes.get(0);
        double sam = sizes.get(1);
        double som = sizes.get(2);
        double growthRate = extractGrowthRate(knowledge);

        List<String> segments = block(context, AnalyzedDimension.TARGET_CUSTOMER);
        List<String> competitors = competitors(context, knowledge);
        List<String> channels = channels(knowledge);
        List<String> barriers = entryBarriers(knowledge);

        double confidence = knowledge == null ? 0.0 : knowledge.confidence();
        boolean offline = knowledge == null || knowledge.isEmpty();

        var plan = MarketPlan.of(tam, sam, som, growthRate,
            competitors, channels, barriers, segments, confidence);

        String explanation = offline
            ? "Plan de mercado en modo offline: sin hechos externos verificados."
            : "Plan de mercado construido a partir de hechos externos verificados.";

        return new MarketResult(plan, confidence, explanation,
            "MarketEngine", ENGINE_VERSION);
    }

    private List<Double> extractSizes(KnowledgeResult knowledge) {
        var sizes = new ArrayList<Double>();
        if (knowledge != null) {
            for (var fact : knowledge.facts()) {
                var number = firstNumber(fact.claim());
                if (number.isPresent() && sizes.size() < 3) {
                    sizes.add(number.getAsDouble());
                }
            }
        }
        while (sizes.size() < 3) {
            sizes.add(0.0);
        }
        return List.copyOf(sizes);
    }

    private double extractGrowthRate(KnowledgeResult knowledge) {
        if (knowledge != null) {
            for (var fact : knowledge.facts()) {
                var claim = fact.claim();
                if (claim != null && isGrowthClaim(claim)) {
                    var number = firstNumber(claim);
                    if (number.isPresent()) {
                        return number.getAsDouble();
                    }
                }
            }
            for (var fact : knowledge.facts()) {
                var number = firstNumber(fact.claim());
                if (number.isPresent()) {
                    return number.getAsDouble();
                }
            }
        }
        return 0.0;
    }

    private boolean isGrowthClaim(String claim) {
        String lower = claim.toLowerCase();
        return lower.contains("crecimiento") || lower.contains("crece")
            || lower.contains("%");
    }

    private java.util.OptionalDouble firstNumber(String claim) {
        if (claim == null) {
            return java.util.OptionalDouble.empty();
        }
        Matcher matcher = NUMBER_PATTERN.matcher(claim);
        if (matcher.find()) {
            return java.util.OptionalDouble.of(Double.parseDouble(matcher.group()));
        }
        return java.util.OptionalDouble.empty();
    }

    private List<String> block(ProjectContext context, AnalyzedDimension dimension) {
        String value = context.value(dimension);
        if (value == null || value.isBlank()) {
            return List.of(UNDEFINED);
        }
        return splitText(value);
    }

    private List<String> competitors(ProjectContext context, KnowledgeResult knowledge) {
        var list = new ArrayList<String>();
        String competition = context.value(AnalyzedDimension.COMPETITION);
        if (competition != null && !competition.isBlank()) {
            list.addAll(splitText(competition));
        }
        if (knowledge != null) {
            for (var fact : knowledge.facts()) {
                if (fact.claim() != null && !fact.claim().isBlank()) {
                    list.add(fact.claim());
                }
            }
        }
        return list.isEmpty() ? List.of(UNDEFINED) : List.copyOf(list);
    }

    private List<String> channels(KnowledgeResult knowledge) {
        var list = facts(knowledge);
        return list.isEmpty() ? List.of(UNDEFINED) : list;
    }

    private List<String> entryBarriers(KnowledgeResult knowledge) {
        var list = facts(knowledge);
        return list.isEmpty() ? List.of(UNDEFINED) : list;
    }

    private List<String> facts(KnowledgeResult knowledge) {
        if (knowledge == null) {
            return List.of();
        }
        var list = new ArrayList<String>();
        for (var fact : knowledge.facts()) {
            if (fact.claim() != null && !fact.claim().isBlank()) {
                list.add(fact.claim());
            }
        }
        return List.copyOf(list);
    }

    private List<String> splitText(String value) {
        var lines = new ArrayList<String>();
        for (String line : value.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        return lines.isEmpty() ? List.of(UNDEFINED) : List.copyOf(lines);
    }
}
