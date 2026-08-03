package com.kinplatform.kin.enterprise.assembler;

import com.kinplatform.kin.enterprise.engine.result.BusinessModelResult;
import com.kinplatform.kin.enterprise.engine.result.FinancialPlanResult;
import com.kinplatform.kin.enterprise.engine.result.InnovationResult;
import com.kinplatform.kin.enterprise.engine.result.KpiResult;
import com.kinplatform.kin.enterprise.engine.result.MarketResult;
import com.kinplatform.kin.enterprise.engine.result.RiskPlanResult;
import com.kinplatform.kin.enterprise.engine.result.RoadmapResult;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.FinancialPlan;
import com.kinplatform.kin.enterprise.valueobjects.InnovationLevel;
import com.kinplatform.kin.enterprise.valueobjects.InnovationPlan;
import com.kinplatform.kin.enterprise.valueobjects.KpiSet;
import com.kinplatform.kin.enterprise.valueobjects.LeanCanvas;
import com.kinplatform.kin.enterprise.valueobjects.MarketPlan;
import com.kinplatform.kin.enterprise.valueobjects.RiskMatrix;
import com.kinplatform.kin.enterprise.valueobjects.RiskSeverity;
import com.kinplatform.kin.enterprise.valueobjects.RiskStatus;
import com.kinplatform.kin.enterprise.valueobjects.Roadmap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseDocumentAssemblerTest {

    private final EnterpriseDocumentAssembler assembler = new EnterpriseDocumentAssembler();

    private BusinessModelResult businessModel() {
        var canvas = LeanCanvas.of(
            List.of("Problema del cliente"), List.of("Pymes retail"),
            List.of("Ahorro operativo"), List.of("Plataforma SaaS"),
            List.of("Directo"), List.of("Suscripción mensual"),
            List.of("Equipo"), List.of("MRR"), List.of("Datos propios"));
        return new BusinessModelResult(canvas, 0.9, "Lean canvas", "BusinessModelEngine", "1.0.0");
    }

    private MarketResult market() {
        var plan = MarketPlan.of(1_000_000.0, 500_000.0, 120_000.0, 15.0,
            List.of("Competidor A"), List.of("Online"), List.of("Regulación"),
            List.of("Pymes retail"), 0.8);
        return new MarketResult(plan, 0.8, "Plan de mercado", "MarketEngine", "1.0.0");
    }

    private InnovationResult innovation() {
        var plan = InnovationPlan.of(InnovationLevel.TRANSFORMATIONAL,
            List.of("Diferenciador"), "Ventaja", List.of("I+D 2026"),
            List.of("Investigación de mercado"));
        return new InnovationResult(plan, 0.7, "Plan de innovación", "InnovationEngine", "1.0.0");
    }

    private FinancialPlanResult financialPlan() {
        var scenario = FinancialPlan.Scenario.of(150_000.0, 60.0);
        var plan = FinancialPlan.of(30_000.0, 48_000.0, 120_000.0, 138_000.0, 158_700.0,
            10, 60.0, scenario, scenario, scenario);
        return new FinancialPlanResult(plan, 0.8, "Plan financiero", "FinancialPlanEngine", "1.0.0");
    }

    private RoadmapResult roadmap() {
        var roadmap = Roadmap.of(List.of("Validación", "Producto"),
            List.of("Validación completado (mes 3)"), "Horizonte de 12 meses",
            List.of("Validación precede a Producto"),
            List.of(Roadmap.GanttEntry.of("Validación", 1, 3)));
        return new RoadmapResult(roadmap, 0.8, "Hoja de ruta", "RoadmapEngine", "1.0.0");
    }

    private RiskPlanResult riskPlan() {
        var matrix = RiskMatrix.of(List.of(RiskMatrix.Risk.of(
            0.75, 0.7, RiskSeverity.HIGH, "Mitigación", "Fundador", RiskStatus.IDENTIFIED)));
        return new RiskPlanResult(matrix, 0.8, "Matriz de riesgos", "RiskPlanEngine", "1.0.0");
    }

    private KpiResult kpi() {
        var kpi = KpiSet.Kpi.of("Adquisición", 120_000.0, 0.0, "SOM anual", "Anual");
        var kpis = KpiSet.of(List.of(kpi), List.of(), List.of(), List.of(), List.of());
        return new KpiResult(kpis, 0.8, "KPIs", "KpiEngine", "1.0.0");
    }

    @Test
    void assembleConTodosLosResultados_produceSieteDocumentos() {
        var artifacts = assembler.assemble(1, businessModel(), market(), innovation(),
            financialPlan(), roadmap(), riskPlan(), kpi());

        assertEquals(7, artifacts.size());
        var types = artifacts.stream().map(DocumentArtifact::type).collect(Collectors.toSet());
        assertEquals(List.of(DocumentType.LEAN_CANVAS, DocumentType.MARKET_PLAN,
            DocumentType.INNOVATION_PLAN, DocumentType.FINANCIAL_PLAN,
            DocumentType.ROADMAP, DocumentType.RISK_MATRIX, DocumentType.KPI).size(), types.size());
        assertTrue(types.contains(DocumentType.LEAN_CANVAS));
        assertTrue(types.contains(DocumentType.MARKET_PLAN));
        assertTrue(types.contains(DocumentType.INNOVATION_PLAN));
        assertTrue(types.contains(DocumentType.FINANCIAL_PLAN));
        assertTrue(types.contains(DocumentType.ROADMAP));
        assertTrue(types.contains(DocumentType.RISK_MATRIX));
        assertTrue(types.contains(DocumentType.KPI));
    }

    @Test
    void assemble_portaProvenienciaYVersion() {
        var artifacts = assembler.assemble(3, businessModel(), market(), innovation(),
            financialPlan(), roadmap(), riskPlan(), kpi());

        for (DocumentArtifact artifact : artifacts) {
            assertEquals(3, artifact.version());
            assertFalse(artifact.generatedBy().isBlank());
            assertEquals("1.0.0", artifact.engineVersion());
            assertFalse(artifact.inputHash().isBlank());
            assertFalse(artifact.content().isBlank());
            assertNotEquals(0L, artifact.size());
            assertTrue(artifact.metadata().isEmpty());
        }
    }

    @Test
    void assemble_contenidoIncluyeValoresDelModelo() {
        var artifacts = assembler.assemble(1, businessModel(), null, null,
            null, null, null, null);

        assertEquals(1, artifacts.size());
        var content = artifacts.get(0).content();
        assertTrue(content.contains("Problema del cliente"));
        assertTrue(content.contains("customerSegments"));
        assertTrue(content.contains("Pymes retail"));
    }

    @Test
    void assembleResultadoVacio_seOmiteElDocumento() {
        var artifacts = assembler.assemble(1, BusinessModelResult.empty(), market(),
            innovation(), financialPlan(), roadmap(), riskPlan(), kpi());

        assertEquals(6, artifacts.size());
        var types = artifacts.stream().map(DocumentArtifact::type).collect(Collectors.toSet());
        assertFalse(types.contains(DocumentType.LEAN_CANVAS));
    }

    @Test
    void assembleResultadoNulo_seOmiteElDocumento() {
        var artifacts = assembler.assemble(1, null, null, innovation(),
            financialPlan(), roadmap(), riskPlan(), kpi());

        assertEquals(5, artifacts.size());
    }

    @Test
    void assembleSinResultados_produceListaVacia() {
        var artifacts = assembler.assemble(1, null, null, null, null, null, null, null);
        assertTrue(artifacts.isEmpty());
    }

    @Test
    void assembleTodosVacios_produceListaVacia() {
        var artifacts = assembler.assemble(1, BusinessModelResult.empty(), MarketResult.empty(),
            InnovationResult.empty(), FinancialPlanResult.empty(), RoadmapResult.empty(),
            RiskPlanResult.empty(), KpiResult.empty());
        assertTrue(artifacts.isEmpty());
    }

    @Test
    void assembleMismoInput_produceMismoContenido() {
        var first = assembler.assemble(1, businessModel(), market(), innovation(),
            financialPlan(), roadmap(), riskPlan(), kpi());
        var second = assembler.assemble(1, businessModel(), market(), innovation(),
            financialPlan(), roadmap(), riskPlan(), kpi());

        var firstByType = first.stream().collect(Collectors.toMap(DocumentArtifact::type, a -> a));
        var secondByType = second.stream().collect(Collectors.toMap(DocumentArtifact::type, a -> a));
        for (Map.Entry<DocumentType, DocumentArtifact> entry : firstByType.entrySet()) {
            assertEquals(entry.getValue().content(),
                secondByType.get(entry.getKey()).content());
            assertEquals(entry.getValue().inputHash(),
                secondByType.get(entry.getKey()).inputHash());
        }
    }
}
