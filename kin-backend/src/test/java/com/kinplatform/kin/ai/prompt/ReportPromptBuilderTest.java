package com.kinplatform.kin.ai.prompt;

import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.ai.prompt.formatter.ExecutiveSummaryFormatter;
import com.kinplatform.kin.ai.prompt.formatter.FinancialSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.InnovationSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.MarketSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.NextStepsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.OpportunitiesSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RecommendationsSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ReportMetadataFormatter;
import com.kinplatform.kin.ai.prompt.formatter.RisksSectionFormatter;
import com.kinplatform.kin.ai.prompt.formatter.ScoresSectionFormatter;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.EffortLevel;
import com.kinplatform.kin.reporting.ImpactLevel;
import com.kinplatform.kin.reporting.Recommendation;
import com.kinplatform.kin.reporting.RecommendationCategory;
import com.kinplatform.kin.reporting.RecommendationExplanation;
import com.kinplatform.kin.reporting.opportunity.Opportunity;
import com.kinplatform.kin.reporting.opportunity.OpportunityCategory;
import com.kinplatform.kin.reporting.opportunity.OpportunityExplanation;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.NextStep;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.ReportSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import com.kinplatform.kin.reporting.risk.Risk;
import com.kinplatform.kin.reporting.risk.RiskCategory;
import com.kinplatform.kin.reporting.risk.RiskExplanation;
import com.kinplatform.kin.reporting.risk.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportPromptBuilderTest {

    private static final List<String> MARKERS_IN_ORDER = List.of(
        "## Resumen Ejecutivo",
        "## Scoring de Viabilidad",
        "## Recomendaciones",
        "## Análisis de Riesgos",
        "## Oportunidades Identificadas",
        "## Proyección Financiera",
        "## Análisis de Mercado",
        "## Innovación",
        "## Próximos Pasos",
        "## Metadata del Reporte"
    );

    private List<SectionFormatter<?>> formatters;
    private ReportPromptBuilder builder;

    @BeforeEach
    void setUp() {
        formatters = List.of(
            new ExecutiveSummaryFormatter(),
            new ScoresSectionFormatter(),
            new RecommendationsSectionFormatter(),
            new RisksSectionFormatter(),
            new OpportunitiesSectionFormatter(),
            new FinancialSectionFormatter(),
            new MarketSectionFormatter(),
            new InnovationSectionFormatter(),
            new NextStepsSectionFormatter(),
            new ReportMetadataFormatter()
        );
        builder = new ReportPromptBuilder(formatters);
    }

    private ConsultingReport populatedReport() {
        var recommendation = Recommendation.create(RecommendationCategory.VALIDATION, "Validar demanda",
            "hablar con clientes", 8, ImpactLevel.HIGH, EffortLevel.LOW, AnalyzedDimension.MVP,
            List.of("encuestar"), "demanda confirmada",
            RecommendationExplanation.of(List.of(), "R1", "validar antes de invertir"));

        var risk = Risk.create(RiskCategory.BUSINESS, "Mercado chico", "poca demanda",
            RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW, 0.7,
            RiskExplanation.of(List.of(), "RK1", "depende de la ciudad", "datos"),
            List.of(), AnalyzedDimension.COMPETITION, "v1");

        var opportunity = Opportunity.create(OpportunityCategory.MERCADO, "Expandir a otras ciudades",
            "mismo modelo en otras plazas", 7, ImpactLevel.MEDIUM, EffortLevel.HIGH, 0.6,
            OpportunityExplanation.of(List.of(), "OP1", "demanda comprobada", "datos"),
            List.of(), AnalyzedDimension.TARGET_CUSTOMER, "v1");

        var coverage = List.of(new DimensionCoverage(AnalyzedDimension.REVENUE_MODEL, true));

        return new ConsultingReport(
            UUID.randomUUID(), UUID.randomUUID(),
            new ExecutiveSummary("Mi App", "Software", 85, 100, "VIABLE", 75.0,
                "Resumen breve del proyecto", List.of("Alta cobertura")),
            new ScoresSection(85, 100, Map.of("Mercado", 85), "VIABLE", 85.0,
                List.of("Propuesta clara"), List.of("Falta presupuesto"), "v1.0"),
            new RecommendationsSection(List.of(recommendation), 8, 0.85,
                RecommendationCategory.VALIDATION),
            new RisksSection(List.of(risk), RiskLevel.HIGH, List.of(risk), 0.7),
            new OpportunitiesSection(List.of(opportunity), List.of(opportunity), 0.6),
            new FinancialSection("Suscripción mensual", "Servidor x4", "Break-even en 12 meses", coverage),
            new MarketSection("Software", "Pymes", "Rosario", "Falta gestión simple", coverage),
            new InnovationSection("App SaaS", "Gestión simple", "MVP con 1 módulo",
                List.of("Automatización"), coverage),
            new NextStepsSection(List.of(
                NextStep.of(NextStep.SOURCE_RECOMMENDATION, "Validar demanda", 8, "confirmar interés"),
                NextStep.of(NextStep.SOURCE_OPPORTUNITY, "Pilotar en una ciudad", 7, "probar aceptación"))),
            new ReportMetadata("2.0.0", "2.0.0-alpha.1",
                OffsetDateTime.of(2026, 7, 31, 12, 0, 0, 0, ZoneOffset.UTC),
                "ReportEngine", Map.of("ScoringEngine", "v1.0"), 75.0, 0.85,
                List.of("ExecutiveSummary", "Scores"))
        );
    }

    @Test
    void constructor_deberiaRechazarListaVacia() {
        assertThrows(IllegalArgumentException.class, () -> new ReportPromptBuilder(List.of()));
    }

    @Test
    void constructor_deberiaRechazarFormattersFaltantes() {
        var sinScores = formatters.stream()
            .filter(f -> !(f instanceof ScoresSectionFormatter))
            .toList();
        assertThrows(IllegalArgumentException.class, () -> new ReportPromptBuilder(sinScores));
    }

    @Test
    void constructor_deberiaRechazarFormattersDuplicados() {
        var duplicados = new java.util.ArrayList<>(formatters);
        duplicados.add(new ScoresSectionFormatter());
        assertThrows(IllegalArgumentException.class, () -> new ReportPromptBuilder(duplicados));
    }

    @Test
    void build_deberiaRechazarTipoConversation() {
        var ctx = com.kinplatform.kin.context.ProjectContext.fromProject("Mi App", null, "Software");
        var decision = com.kinplatform.kin.decision.ConversationDecision.ask(
            AnalyzedDimension.PROBLEM, 10, "explorar problema");
        var request = PromptRequest.forConversation(ctx, decision);
        var ex = assertThrows(IllegalArgumentException.class, () -> builder.build(request));
        assertEquals("ReportPromptBuilder solo soporta REPORT", ex.getMessage());
    }

    @Test
    void build_deberiaIncluirCabeceraYInstruccionFinal() {
        var prompt = builder.build(PromptRequest.forReport(populatedReport()));

        assertTrue(prompt.contains("=== CONSULTING REPORT ==="));
        assertTrue(prompt.contains("--- INSTRUCCIÓN PARA EL LLM ---"));
        assertTrue(prompt.contains("Eres KIN. Explica el reporte anterior de forma natural"));
        assertTrue(prompt.contains("No añadas secciones nuevas. No recalcules scores."));
    }

    @Test
    void build_deberiaIncluirCadaSeccionExactamenteUnaVez() {
        var prompt = builder.build(PromptRequest.forReport(populatedReport()));

        for (String marker : MARKERS_IN_ORDER) {
            assertEquals(1, countOccurrences(prompt, marker),
                "La sección '" + marker + "' debe aparecer exactamente una vez");
        }
    }

    @Test
    void build_deberiaRespetarElOrdenDeSectionsInOrder() {
        var prompt = builder.build(PromptRequest.forReport(populatedReport()));

        int lastIndex = -1;
        for (String marker : MARKERS_IN_ORDER) {
            int index = prompt.indexOf(marker);
            assertTrue(index > lastIndex,
                "La sección '" + marker + "' debe aparecer después de las anteriores");
            lastIndex = index;
        }

        var report = populatedReport();
        assertEquals(report.sectionsInOrder().size(), MARKERS_IN_ORDER.size());
    }

    @Test
    void build_deberiaDespacharPorClaseConcreta_noPorReportSectionKind() {
        var report = populatedReport();
        var prompt = builder.build(PromptRequest.forReport(report));

        assertTrue(report.recommendations().kind() == ReportSectionKind.ANALYTIC);
        assertTrue(report.risks().kind() == ReportSectionKind.ANALYTIC);
        assertTrue(report.opportunities().kind() == ReportSectionKind.ANALYTIC);

        assertTrue(prompt.contains("## Recomendaciones"));
        assertTrue(prompt.contains("## Análisis de Riesgos"));
        assertTrue(prompt.contains("## Oportunidades Identificadas"));
        assertTrue(prompt.contains("### 1. Validar demanda"));
        assertTrue(prompt.contains("#### 1. Mercado chico"));
        assertTrue(prompt.contains("### 1. Expandir a otras ciudades"));
    }

    @Test
    void build_deberiaAsignarCadaFormatterAUnaSeccionConcreta() {
        var prompt = builder.build(PromptRequest.forReport(populatedReport()));

        assertTrue(prompt.contains("**Proyecto:** Mi App"));
        assertTrue(prompt.contains("**Total:** 85 / 100"));
        assertTrue(prompt.contains("**Prioridad global:** 8/10"));
        assertTrue(prompt.contains("**Nivel global:** HIGH"));
        assertTrue(prompt.contains("**Total:** 1"));
        assertTrue(prompt.contains("**Modelo de ingresos:** Suscripción mensual"));
        assertTrue(prompt.contains("**Sector:** Software"));
        assertTrue(prompt.contains("**Solución:** App SaaS"));
        assertTrue(prompt.contains("**Total:** 2 pasos siguientes"));
        assertTrue(prompt.contains("**Versión del reporte:** 2.0.0"));
    }

    @Test
    void format_deberiaFallarParaSeccionDesconocida() throws Exception {
        var method = ReportPromptBuilder.class.getDeclaredMethod("format", ReportSection.class);
        method.setAccessible(true);
        var ex = assertThrows(InvocationTargetException.class,
            () -> method.invoke(builder, new UnknownSection()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("No existe formatter para sección Unknown"));
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static final class UnknownSection implements ReportSection {
        @Override
        public String sectionName() {
            return "Unknown";
        }

        @Override
        public ReportSectionKind kind() {
            return ReportSectionKind.GENERAL;
        }
    }
}
