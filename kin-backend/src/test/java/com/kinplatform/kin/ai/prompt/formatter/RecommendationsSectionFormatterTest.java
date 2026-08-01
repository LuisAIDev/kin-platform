package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationCategory;
import com.kinplatform.kin.reporting.RecommendationExplanation;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationsSectionFormatterTest {

    private final RecommendationsSectionFormatter formatter = new RecommendationsSectionFormatter();

    private Recommendation rec(String title, String reason) {
        return Recommendation.create(RecommendationCategory.VALIDATION, title, "descripción",
            7, ImpactLevel.MEDIUM, EffortLevel.LOW, AnalyzedDimension.MVP,
            List.of("paso 1"), "resultado esperado",
            RecommendationExplanation.of(List.of(), "regla", reason));
    }

    @Test
    void kind_deberiaSerAnalytic() {
        assertEquals(ReportSectionKind.ANALYTIC, formatter.kind());
    }

    @Test
    void format_deberiaIncluirRecomendacionesConPasosYExplicacion() {
        var section = new RecommendationsSection(
            List.of(rec("Validar con usuarios", "necesario validar")),
            8, 0.85, RecommendationCategory.VALIDATION);

        var result = formatter.format(section);

        assertTrue(result.contains("## Recomendaciones"));
        assertTrue(result.contains("**Prioridad global:** 8/10"));
        assertTrue(result.contains("**Confianza:** 85.0%"));
        assertTrue(result.contains("**Categoría dominante:** VALIDATION"));
        assertTrue(result.contains("### 1. Validar con usuarios"));
        assertTrue(result.contains("**Categoría:** VALIDATION"));
        assertTrue(result.contains("**Prioridad:** 7/10"));
        assertTrue(result.contains("**Impacto:** MEDIUM"));
        assertTrue(result.contains("**Esfuerzo:** LOW"));
        assertTrue(result.contains("**Descripción:** descripción"));
        assertTrue(result.contains("**Pasos accionables:**"));
        assertTrue(result.contains("- paso 1"));
        assertTrue(result.contains("**Resultado esperado:** resultado esperado"));
        assertTrue(result.contains("_necesario validar_"));
    }

    @Test
    void format_deberiaIncluirTextoDefault_cuandoNoHayPasosNiExplicacion() {
        var rec = Recommendation.create(RecommendationCategory.MARKETING, "Promocionar",
            "desc", 5, ImpactLevel.LOW, EffortLevel.MEDIUM, AnalyzedDimension.TARGET_CUSTOMER,
            List.of(), "", RecommendationExplanation.of(List.of(), "r", ""));

        var result = formatter.format(new RecommendationsSection(List.of(rec), 5, 0.5,
            RecommendationCategory.MARKETING));

        assertTrue(result.contains("### 1. Promocionar"));
        assertFalse(result.contains("**Pasos accionables:**"));
        assertFalse(result.contains("_"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoNoHayRecomendaciones() {
        var result = formatter.format(RecommendationsSection.empty());

        assertTrue(result.contains("## Recomendaciones"));
        assertTrue(result.contains("_Sin recomendaciones generadas._"));
    }
}
