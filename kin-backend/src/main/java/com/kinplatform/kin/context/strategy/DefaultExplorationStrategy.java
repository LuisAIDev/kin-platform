package com.kinplatform.kin.context.strategy;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;

import java.util.EnumMap;
import java.util.Map;

public class DefaultExplorationStrategy implements ExplorationStrategy {

    private final ExplorationPriority priorityOrder;

    private static final Map<AnalyzedDimension, String> INSTRUCTIONS = buildInstructions();

    public DefaultExplorationStrategy(ExplorationPriority priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    @Override
    public ConversationDecision decide(ProjectContext context, CompletenessEvaluation evaluation) {
        if (evaluation.readyForReport()) {
            return ConversationDecision.generateReport("Cobertura y confianza suficientes para generar informe");
        }

        var missing = evaluation.missingDimensions();
        if (missing.isEmpty()) {
            return ConversationDecision.generateReport("Todas las dimensiones han sido cubiertas");
        }

        var adjustedPriority = adjustPriorityByContext(context);
        var next = adjustedPriority.highestPriority(missing);
        if (next == null) {
            return ConversationDecision.generateReport("No hay m\u00E1s dimensiones prioritarias por explorar");
        }

        return ConversationDecision.ask(
            next,
            adjustedPriority.getPriority(next),
            INSTRUCTIONS.getOrDefault(next, "Explorar " + next.displayName())
        );
    }

    ExplorationPriority adjustPriorityByContext(ProjectContext context) {
        return priorityOrder;
    }

    private static Map<AnalyzedDimension, String> buildInstructions() {
        var map = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);
        map.put(AnalyzedDimension.PROBLEM, "Indagar sobre el problema o necesidad espec\u00EDfica que el proyecto resuelve");
        map.put(AnalyzedDimension.SOLUTION, "Explorar la soluci\u00F3n propuesta en detalle");
        map.put(AnalyzedDimension.TARGET_CUSTOMER, "Preguntar sobre el cliente o usuario objetivo");
        map.put(AnalyzedDimension.VALUE_PROPOSITION, "Indagar sobre la propuesta de valor \u00FAnica");
        map.put(AnalyzedDimension.REVENUE_MODEL, "Preguntar sobre el modelo de ingresos o monetizaci\u00F3n");
        map.put(AnalyzedDimension.COMPETITION, "Explorar el panorama competitivo y diferenciaci\u00F3n");
        map.put(AnalyzedDimension.RISKS, "Preguntar sobre los riesgos identificados");
        map.put(AnalyzedDimension.RESOURCES, "Indagar sobre los recursos necesarios para ejecutar el proyecto");
        map.put(AnalyzedDimension.MVP, "Preguntar sobre el plan de validaci\u00F3n o prototipo m\u00EDnimo");
        map.put(AnalyzedDimension.SCALABILITY, "Explorar el potencial de escalabilidad del proyecto");
        map.put(AnalyzedDimension.OBJECTIVES, "Preguntar sobre los objetivos y metas del proyecto");
        map.put(AnalyzedDimension.SECTOR, "Precisar el sector o industria espec\u00EDfica");
        map.put(AnalyzedDimension.CITY, "Confirmar la ubicaci\u00F3n o mercado geogr\u00E1fico");
        map.put(AnalyzedDimension.PROJECT_NAME, "Confirmar el nombre del proyecto");
        return Map.copyOf(map);
    }
}
