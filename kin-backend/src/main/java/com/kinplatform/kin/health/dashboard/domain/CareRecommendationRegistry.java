package com.kinplatform.kin.health.dashboard.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro determinista de recomendaciones de cuidado (ADR-030).
 *
 * <p>Fuente de plantillas en Java (dominio puro, sin LLM): asocia condiciones
 * (nombres canónicos del catálogo de triaje) con consejos de cuidado. El
 * {@code DashboardService} consulta este registro para generar el
 * {@link CarePlan} a partir de las condiciones del perfil y del historial.
 * Se puede ampliar en el futuro con un archivo de configuración sin cambiar el
 * contrato.</p>
 */
public final class CareRecommendationRegistry {

    /** Orden de prioridad mostrado (mayor = más importante). */
    private final Map<String, Recommendation> recommendations;

    public CareRecommendationRegistry(Map<String, Recommendation> recommendations) {
        this.recommendations = recommendations == null ? Map.of() : Map.copyOf(recommendations);
    }

    public static CareRecommendationRegistry defaults() {
        var map = new LinkedHashMap<String, Recommendation>();
        map.put(
                "hipertensión",
                new Recommendation(
                        "Hipertensión arterial",
                        "Reduce el consumo de sal, haz ejercicio moderado y controla tu presión regularmente.",
                        "ALTA"));
        map.put("hipertension", map.get("hipertensión"));
        map.put(
                "diabetes",
                new Recommendation(
                        "Diabetes tipo 2",
                        "Controla tu glucosa, sigue una dieta equilibrada y realiza actividad física regular.",
                        "ALTA"));
        map.put(
                "asma",
                new Recommendation(
                        "Asma",
                        "Evita los desencadenantes, usa el inhalador según indicación y acude si empeora la disnea.",
                        "ALTA"));
        map.put(
                "gripe",
                new Recommendation(
                        "Gripe",
                        "Reposo, hidratación abundante y consulta médica si la fiebre persiste más de 3 días.",
                        "MEDIA"));
        map.put(
                "resfriado común",
                new Recommendation(
                        "Resfriado común", "Descansa, hidrátate y usa descongestionantes si es necesario.", "BAJA"));
        map.put(
                "neumonía",
                new Recommendation(
                        "Neumonía",
                        "Acude a urgencias si tienes fiebre alta o dificultad respiratoria. No automedicarse.",
                        "ALTA"));
        map.put(
                "migraña",
                new Recommendation(
                        "Migraña",
                        "Evita desencadenantes, descansa en un lugar oscuro y consulta si las crisis son frecuentes.",
                        "MEDIA"));
        map.put(
                "infección urinaria",
                new Recommendation(
                        "Infección urinaria",
                        "Bebe abundante agua y consulta en 24 h para tratamiento antibiótico.",
                        "MEDIA"));
        map.put(
                "gastritis",
                new Recommendation(
                        "Gastritis",
                        "Evita irritantes, alcohol y comidas copiosas; consulta si el dolor persiste.",
                        "MEDIA"));
        map.put(
                "reflujo gastroesofágico",
                new Recommendation(
                        "Reflujo gastroesofágico",
                        "Evita acostarte tras comer y las comidas copiosas; consulta si persiste.",
                        "BAJA"));
        map.put(
                "anemia",
                new Recommendation("Anemia", "Consulta médica para estudio de causas; dieta rica en hierro.", "MEDIA"));
        map.put(
                "lumbalgia",
                new Recommendation(
                        "Lumbalgia",
                        "Evita el reposo prolongado, aplica calor local y haz ejercicios de estiramiento.",
                        "MEDIA"));
        map.put(
                "artrosis",
                new Recommendation(
                        "Artrosis",
                        "Ejercicio de bajo impacto y control de peso para aliviar las articulaciones.",
                        "MEDIA"));
        map.put(
                "gota",
                new Recommendation(
                        "Gota", "Reduce el alcohol y las purinas; consulta para manejo del ácido úrico.", "MEDIA"));
        map.put(
                "sinusitis",
                new Recommendation(
                        "Sinusitis",
                        "Humidifica el ambiente y usa descongestionantes; consulta si no mejora.",
                        "BAJA"));
        return new CareRecommendationRegistry(map);
    }

    /**
     * Genera el plan de cuidado a partir de condiciones identificadas
     * (perfil + historial). Deduplica por consejo y ordena por prioridad.
     */
    public CarePlan planFor(List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return CarePlan.empty();
        }
        var selected = new LinkedHashMap<String, Recommendation>();
        for (String condition : conditions) {
            if (condition == null || condition.isBlank()) {
                continue;
            }
            Recommendation rec = recommendations.get(condition.strip().toLowerCase());
            if (rec != null && !selected.containsKey(rec.label())) {
                selected.put(rec.label(), rec);
            }
        }
        if (selected.isEmpty()) {
            return CarePlan.empty();
        }
        var detailed = selected.values().stream()
                .map(rec -> new CarePlan.CareRecommendation(rec.label(), rec.advice(), rec.priority()))
                .toList();
        return new CarePlan(
                detailed.stream().map(CarePlan.CareRecommendation::advice).toList(), conditions, detailed);
    }

    /** Recomendación de plantilla (dominio puro). */
    public record Recommendation(String label, String advice, String priority) {}
}
