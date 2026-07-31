package com.kinplatform.kin.decision;

import com.kinplatform.kin.context.AnalyzedDimension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ConversationDecision(
    Action action,
    AnalyzedDimension dimension,
    int priority,
    String explanation,
    Map<String, Object> metadata
) {

    public enum Action {
        ASK,
        REPORT,
        RECOMMEND,
        VALIDATE,
        SUMMARIZE,
        STOP,
        ESCALATE
    }

    public ConversationDecision {
        metadata = (metadata != null)
                ? Collections.unmodifiableMap(new LinkedHashMap<>(metadata))
                : Collections.emptyMap();
    }

    public static ConversationDecision ask(AnalyzedDimension dimension, int priority, String explanation) {
        return new ConversationDecision(Action.ASK, dimension, priority, explanation, Map.of());
    }

    public static ConversationDecision generateReport(String explanation) {
        return new ConversationDecision(Action.REPORT, null, Integer.MAX_VALUE, explanation, Map.of());
    }

    public static ConversationDecision stop(String explanation) {
        return new ConversationDecision(Action.STOP, null, 0, explanation, Map.of());
    }

    public boolean shouldAskQuestion() {
        return action == Action.ASK;
    }

    public boolean shouldGenerateReport() {
        return action == Action.REPORT;
    }

    public String toStrategySnippet() {
        return switch (action) {
            case REPORT -> "Has completado la fase de exploración.\n"
                    + "Acción: Generar el INFORME DE VIABILIDAD completo.\n"
                    + "No hagas más preguntas.\n";
            case ASK -> "Dimensión prioritaria: " + (dimension != null ? dimension.displayName() : "N/A") + "\n"
                    + "Prioridad: " + priority + "/10\n"
                    + "Instrucción: " + explanation + "\n"
                    + "Hacé UNA SOLA PREGUNTA relevante sobre esta dimensión.\n"
                    + "No preguntes sobre otras dimensiones.\n";
            case STOP -> "La conversación ha finalizado.\nNo generes más respuestas.\n";
            default -> "";
        };
    }
}
