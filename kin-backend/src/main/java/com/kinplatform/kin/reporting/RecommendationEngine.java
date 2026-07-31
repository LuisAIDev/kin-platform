package com.kinplatform.kin.reporting;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluation;
import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.scoring.ScoreResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Motor determinista de recomendaciones. Evalúa el contexto del proyecto, la
 * evaluación de completitud y el score de viabilidad para producir
 * recomendaciones estructuradas y auditables.
 *
 * <p>Es un servicio de dominio puro: no depende de Spring, ni de la infraestructura
 * de IA, ni de prompts. Las reglas son 100% deterministas y reproducibles.</p>
 *
 * <p>Implementa {@link DomainEngine} para integrarse con la infraestructura
 * común de motores (registry + executor) sin modificar su lógica.</p>
 */
public class RecommendationEngine implements DomainEngine<RecommendationInput, RecommendationResult> {

    public static final String GENERATOR_NAME = "RecommendationEngine";

    private final RecommendationModel model;

    public RecommendationEngine(RecommendationModel model) {
        this.model = model;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, model.version(), "KIN Architecture Team",
            EnginePhase.RECOMMENDATION, EngineType.DOMAIN, 40);
    }

    @Override
    public RecommendationResult evaluate(RecommendationInput input) {
        if (input == null || input.projectContext() == null || input.evaluation() == null || input.score() == null) {
            return RecommendationResult.empty();
        }

        var recommendations = new ArrayList<Recommendation>();
        recommendations.addAll(coverageRecommendations(input));
        recommendations.addAll(scoreRecommendations(input));
        recommendations.addAll(maturityRecommendations(input));

        recommendations.sort(Comparator.comparingInt(Recommendation::priority).reversed());

        return buildResult(recommendations, input);
    }

    private List<Recommendation> coverageRecommendations(RecommendationInput input) {
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var result = new ArrayList<Recommendation>();

        for (var dimension : project.missingDimensions()) {
            int priority = evaluation.criticalMissingDimensions().contains(dimension) ? 9 : 6;
            result.add(buildCoverageRecommendation(dimension, priority, evaluation));
        }
        return result;
    }

    private Recommendation buildCoverageRecommendation(AnalyzedDimension dimension, int priority,
                                                        CompletenessEvaluation evaluation) {
        var spec = coverageSpec(dimension);
        var rule = "Cobertura de dimensiones: " + dimension.displayName() + " no cubierta";
        var used = List.of(
            "Dimensión analizada: " + dimension.displayName(),
            "Cobertura actual: " + percentage(evaluation.coveragePercent())
        );
        var explanation = RecommendationExplanation.of(used, rule,
            "Completar esta dimensión reduce la incertidumbre del modelo de negocio.");
        return Recommendation.create(spec.category(), spec.title(), spec.description(),
            priority, spec.impact(), spec.effort(), dimension,
            spec.steps(), spec.outcome(), explanation);
    }

    private CoverageSpec coverageSpec(AnalyzedDimension dimension) {
        return switch (dimension) {
            case PROBLEM -> CoverageSpec.of(RecommendationCategory.STRATEGY,
                "Definir claramente el problema que se resuelve",
                "El problema es el ancla del modelo de negocio. Sin una definición precisa, la propuesta de valor carece de dirección.",
                ImpactLevel.HIGH, EffortLevel.LOW,
                List.of("Redactar el problema en una frase medible.",
                    "Describir a quién le duele y con qué frecuencia.",
                    "Cuantificar el costo del problema para el cliente."),
                "Alcance y relevancia del problema bien definidos.");
            case SOLUTION -> CoverageSpec.of(RecommendationCategory.PRODUCT,
                "Describir la solución propuesta con detalle",
                "La solución debe explicar cómo resuelve el problema y en qué se diferencia de alternativas actuales.",
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("Documentar cómo funciona la solución paso a paso.",
                    "Explicar los recursos y capacidades requeridos.",
                    "Identificar los componentes centrales del producto o servicio."),
                "Solución entendible y lista para validar con usuarios.");
            case TARGET_CUSTOMER -> CoverageSpec.of(RecommendationCategory.VALIDATION,
                "Validar el segmento de cliente objetivo",
                "El cliente objetivo determina el mensaje, el canal y la estrategia de adquisición. Validarlo es el primer paso real del proyecto.",
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("Definir el perfil demográfico y conductual del cliente.",
                    "Realizar al menos 10 entrevistas de descubrimiento.",
                    "Identificar el canal donde se concentra el segmento."),
                "Segmento validado con evidencia de demanda real.");
            case VALUE_PROPOSITION -> CoverageSpec.of(RecommendationCategory.STRATEGY,
                "Refinar la propuesta de valor",
                "La propuesta de valor debe conectar el problema con la solución y expresar el beneficio diferencial.",
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("Construir el lienzo de propuesta de valor.",
                    "Explicar los beneficios tangibles para el cliente.",
                    "Diferenciarse explícitamente de las alternativas."),
                "Propuesta de valor clara y diferenciada.");
            case REVENUE_MODEL -> CoverageSpec.of(RecommendationCategory.FINANCIAL,
                "Diseñar el modelo de ingresos",
                "Sin un modelo de ingresos definido la viabilidad financiera es incierta.",
                ImpactLevel.CRITICAL, EffortLevel.MEDIUM,
                List.of("Definir las fuentes de ingresos principales.",
                    "Establecer la estructura de precios.",
                    "Proyectar ingresos a 12 meses."),
                "Modelo de ingresos definido y proyectable.");
            case COMPETITION -> CoverageSpec.of(RecommendationCategory.STRATEGY,
                "Analizar el panorama competitivo",
                "Conocer a los competidores permite posicionar la propuesta y evitar supuestos de mercado.",
                ImpactLevel.HIGH, EffortLevel.MEDIUM,
                List.of("Identificar competidores directos e indirectos.",
                    "Comparar precio, calidad y distribución.",
                    "Definir la ventaja competitiva sostenible."),
                "Estrategia de posicionamiento fundamentada.");
            case RISKS -> CoverageSpec.of(RecommendationCategory.OPERATIONS,
                "Identificar y mitigar los riesgos del proyecto",
                "Los riesgos no gestionados pueden comprometer la ejecución del proyecto.",
                ImpactLevel.CRITICAL, EffortLevel.MEDIUM,
                List.of("Enumerar los riesgos técnicos, de mercado y de operación.",
                    "Asignar probabilidad e impacto a cada riesgo.",
                    "Definir un plan de mitigación por riesgo."),
                "Riesgos identificados con plan de mitigación.");
            case RESOURCES -> CoverageSpec.of(RecommendationCategory.OPERATIONS,
                "Planificar los recursos necesarios",
                "La ejecución depende de contar con los recursos humanos, materiales y financieros adecuados.",
                ImpactLevel.MEDIUM, EffortLevel.MEDIUM,
                List.of("Listar recursos humanos, técnicos y financieros.",
                    "Estimar el presupuesto de arranque.",
                    "Definir el responsable de cada recurso clave."),
                "Plan de recursos que soporta la ejecución.");
            case MVP -> CoverageSpec.of(RecommendationCategory.PRODUCT,
                "Diseñar un MVP de validación temprana",
                "Un MVP permite validar la solución con usuarios reales con la menor inversión posible.",
                ImpactLevel.HIGH, EffortLevel.LOW,
                List.of("Definir la versión mínima que entrega el beneficio central.",
                    "Seleccionar un grupo piloto de usuarios.",
                    "Definir las métricas de validación."),
                "Validación temprana con evidencia de tracción.");
            case SCALABILITY -> CoverageSpec.of(RecommendationCategory.STRATEGY,
                "Evaluar la escalabilidad del modelo",
                "La escalabilidad determina el potencial de crecimiento del proyecto a mediano plazo.",
                ImpactLevel.MEDIUM, EffortLevel.MEDIUM,
                List.of("Analizar si el modelo puede crecer sin costos lineales.",
                    "Identificar las palancas de crecimiento.",
                    "Estimar el mercado potencial."),
                "Modelo con potencial de crecimiento claro.");
            case OBJECTIVES -> CoverageSpec.of(RecommendationCategory.STRATEGY,
                "Definir objetivos medibles",
                "Los objetivos permiten monitorear el avance y tomar decisiones con datos.",
                ImpactLevel.MEDIUM, EffortLevel.LOW,
                List.of("Definir objetivos SMART a 3, 6 y 12 meses.",
                    "Asignar un indicador a cada objetivo.",
                    "Revisar los objetivos periódicamente."),
                "Gestión del proyecto orientada a resultados.");
            case SECTOR -> CoverageSpec.of(RecommendationCategory.MARKETING,
                "Caracterizar el sector del negocio",
                "Conocer el sector permite dimensionar el mercado y las tendencias.",
                ImpactLevel.MEDIUM, EffortLevel.LOW,
                List.of("Describir el sector y sus tendencias.",
                    "Estimar el tamaño del mercado.",
                    "Identificar actores relevantes del sector."),
                "Contexto de mercado dimensionado.");
            case CITY -> CoverageSpec.of(RecommendationCategory.MARKETING,
                "Delimitar la ubicación o mercado objetivo",
                "La ubicación define el alcance geográfico y el plan de expansión.",
                ImpactLevel.LOW, EffortLevel.LOW,
                List.of("Definir la plaza o región de operación.",
                    "Analizar el potencial del mercado local.",
                    "Evaluar la expansión geográfica futura."),
                "Alcance geográfico definido.");
            case PROJECT_NAME -> CoverageSpec.of(RecommendationCategory.STRATEGY,
                "Definir el nombre y posicionamiento del proyecto",
                "Un nombre claro facilita la comunicación y el posicionamiento.",
                ImpactLevel.LOW, EffortLevel.LOW,
                List.of("Elegir un nombre memorable y disponible.",
                    "Definir el mensaje de posicionamiento.",
                    "Registrar la marca si corresponde."),
                "Identidad del proyecto definida.");
            default -> CoverageSpec.of(RecommendationCategory.VALIDATION,
                "Completar la información de esta dimensión",
                "La dimensión aún no está cubierta con información suficiente.",
                ImpactLevel.MEDIUM, EffortLevel.LOW,
                List.of("Completar la información faltante de esta dimensión."),
                "Información de la dimensión completa.");
        };
    }

    private List<Recommendation> scoreRecommendations(RecommendationInput input) {
        var score = input.score();
        var project = input.projectContext();
        var evaluation = input.evaluation();
        var result = new ArrayList<Recommendation>();

        if (score.totalScore() < model.lowScoreThreshold()) {
            var weakest = weakestCoveredDimension(score);
            if (weakest != null) {
                result.add(Recommendation.create(
                    RecommendationCategory.FINANCIAL,
                    "Reforzar el pilar más débil: " + weakest.displayName(),
                    "El score de viabilidad es bajo y esta dimensión es la que más peso resta al resultado global.",
                    8, ImpactLevel.CRITICAL, EffortLevel.MEDIUM, weakest,
                    List.of("Revisar la información actual de la dimensión.",
                        "Complementar con datos reales de mercado.",
                        "Rediseñar la estrategia en torno a esta dimensión."),
                    "El score de viabilidad global aumenta.",
                    RecommendationExplanation.of(
                        List.of("Score total: " + score.totalScore() + "/" + score.maxScore(),
                            "Dimensión con menor puntaje: " + weakest.displayName()),
                        "Score bajo: " + score.totalScore() + " < " + model.lowScoreThreshold(),
                        "Fortalecer el eslabón más débil eleva la viabilidad del proyecto.")));
            } else {
                result.add(Recommendation.create(
                    RecommendationCategory.VALIDATION,
                    "Recolectar información básica del proyecto",
                    "No hay suficiente información para evaluar la viabilidad; se requiere un trabajo inicial de definición.",
                    8, ImpactLevel.HIGH, EffortLevel.LOW, null,
                    List.of("Completar problema, solución y cliente objetivo.",
                        "Realizar entrevistas de descubrimiento."),
                    "Base de información mínima para evaluar el proyecto.",
                    RecommendationExplanation.of(
                        List.of("Score total: " + score.totalScore() + "/" + score.maxScore()),
                        "Score bajo y sin dimensiones cubiertas con puntaje.",
                        "Sin información no es posible generar recomendaciones específicas.")));
            }
        }

        if (score.totalScore() >= model.highScoreThreshold() && !project.missingDimensions().isEmpty()) {
            result.add(Recommendation.create(
                RecommendationCategory.INNOVATION,
                "Buscar diferenciación e innovación sostenible",
                "El proyecto tiene un score alto; conviene sostenerlo con innovación continua y diferenciación.",
                5, ImpactLevel.MEDIUM, EffortLevel.MEDIUM, null,
                List.of("Explorar nuevas líneas de innovación.",
                    "Monitorear a la competencia constantemente.",
                    "Invertir en mejoras diferenciales."),
                "Ventaja competitiva sostenible en el tiempo.",
                RecommendationExplanation.of(
                    List.of("Score total: " + score.totalScore() + "/" + score.maxScore(),
                        "Cobertura: " + percentage(evaluation.coveragePercent())),
                    "Score alto: " + score.totalScore() + " >= " + model.highScoreThreshold(),
                    "Un score alto debe sostenerse con mejora continua.")));
        }

        return result;
    }

    private List<Recommendation> maturityRecommendations(RecommendationInput input) {
        var maturity = input.evaluation().maturityLevel();
        var result = new ArrayList<Recommendation>();

        if (maturity == CompletenessEvaluation.MaturityLevel.EARLY) {
            result.add(Recommendation.create(
                RecommendationCategory.VALIDATION,
                "Priorizar la validación temprana del cliente",
                "Un proyecto en etapa temprana debe validar el problema y la disposición a pagar antes de escalar.",
                7, ImpactLevel.HIGH, EffortLevel.LOW, AnalyzedDimension.TARGET_CUSTOMER,
                List.of("Ejecutar entrevistas con usuarios potenciales.",
                    "Validar disposición a pagar.",
                    "Iterar la propuesta de valor con la evidencia."),
                "Riesgo de mercado reducido antes de invertir recursos.",
                RecommendationExplanation.of(
                    List.of("Madurez del proyecto: " + maturity.name(),
                        "Cobertura: " + percentage(input.evaluation().coveragePercent())),
                    "Madurez temprana: " + maturity.name(),
                    "En etapas tempranas el mayor riesgo es construir algo que nadie quiere.")));
        }

        if (maturity == CompletenessEvaluation.MaturityLevel.MATURE
            && !input.projectContext().missingDimensions().isEmpty()) {
            result.add(Recommendation.create(
                RecommendationCategory.STRATEGY,
                "Consolidar el plan de escalamiento",
                "El proyecto está maduro; el siguiente paso es definir cómo escalar de forma sostenible.",
                5, ImpactLevel.MEDIUM, EffortLevel.HIGH, AnalyzedDimension.SCALABILITY,
                List.of("Definir el plan de crecimiento a 12 meses.",
                    "Evaluar la estructura de costos para escalar.",
                    "Definir alianzas estratégicas."),
                "Crecimiento sostenible y planificado.",
                RecommendationExplanation.of(
                    List.of("Madurez del proyecto: " + maturity.name()),
                    "Madurez madura con dimensiones pendientes",
                    "Un proyecto maduro con brechas restantes debe cerrarlas mientras escala.")));
        }

        return result;
    }

    private AnalyzedDimension weakestCoveredDimension(ScoreResult score) {
        AnalyzedDimension weakest = null;
        int minScore = Integer.MAX_VALUE;
        for (var entry : score.categoryScores().entrySet()) {
            var dimension = findDimension(entry.getKey());
            if (dimension == null) continue;
            if (entry.getValue() < minScore && entry.getValue() > 0) {
                minScore = entry.getValue();
                weakest = dimension;
            }
        }
        return weakest;
    }

    private AnalyzedDimension findDimension(String displayName) {
        for (var dim : AnalyzedDimension.values()) {
            if (dim.displayName().equals(displayName)) return dim;
        }
        return null;
    }

    private RecommendationResult buildResult(List<Recommendation> recommendations, RecommendationInput input) {
        int priority = recommendations.stream().mapToInt(Recommendation::priority).max().orElse(0);
        var category = dominantCategory(recommendations);
        double confidence = computeConfidence(input);
        var explanation = buildExplanation(recommendations, input, confidence);

        return new RecommendationResult(
            recommendations, priority, confidence, category,
            explanation, GENERATOR_NAME, model.version());
    }

    private double computeConfidence(RecommendationInput input) {
        var evaluation = input.evaluation();
        var score = input.score();
        double raw = 0.15
            + 0.35 * evaluation.coveragePercent()
            + 0.25 * evaluation.qualityOfInformation()
            + 0.25 * (score.totalScore() / 100.0);
        return Math.max(0.0, Math.min(1.0, raw));
    }

    private String buildExplanation(List<Recommendation> recommendations, RecommendationInput input,
                                    double confidence) {
        if (recommendations.isEmpty()) {
            return "No se identificaron oportunidades de mejora relevantes para el proyecto.";
        }
        int coverageCount = coverageRecommendations(input).size();
        int scoreCount = scoreRecommendations(input).size();
        int maturityCount = maturityRecommendations(input).size();
        return String.format(Locale.ROOT,
            "Se generaron %d recomendaciones (%d por cobertura de dimensiones, %d por score, %d por madurez) "
                + "con una confianza de %.0f%%.",
            recommendations.size(), coverageCount, scoreCount, maturityCount, confidence * 100);
    }

    private RecommendationCategory dominantCategory(List<Recommendation> recommendations) {
        var counts = new LinkedHashMap<RecommendationCategory, Integer>();
        for (var r : recommendations) {
            counts.merge(r.category(), 1, Integer::sum);
        }
        RecommendationCategory dominant = RecommendationCategory.VALIDATION;
        int max = -1;
        for (var entry : counts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    private static String percentage(double ratio) {
        return Math.round(ratio * 100) + "%";
    }

    private record CoverageSpec(RecommendationCategory category, String title, String description,
                                ImpactLevel impact, EffortLevel effort, List<String> steps, String outcome) {
        static CoverageSpec of(RecommendationCategory category, String title, String description,
                               ImpactLevel impact, EffortLevel effort, List<String> steps, String outcome) {
            return new CoverageSpec(category, title, description, impact, effort, steps, outcome);
        }
    }
}
