package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.application.EnterpriseDocumentBundle;
import com.kinplatform.kin.enterprise.engine.EngineTestFixtures;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import com.kinplatform.kin.enterprise.web.dto.EnterpriseGenerateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseWebMapperTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private final EnterpriseWebMapper mapper = new EnterpriseWebMapper();

    @Test
    void toResponse_deberiaMapearLaVersionCompleta() {
        var project = WebTestFixtures.completed(PROJECT_ID, 2, DocumentType.LEAN_CANVAS, DocumentType.KPI);

        var dto = mapper.toResponse(project);

        assertEquals(PROJECT_ID, dto.projectId());
        assertEquals(2, dto.version());
        assertEquals("COMPLETED", dto.status());
        assertEquals(project.createdAt(), dto.createdAt());
        assertEquals(project.completedAt(), dto.completedAt());
        assertEquals(2, dto.documentCount());
        assertEquals(2, dto.documents().size());
        assertEquals("LEAN_CANVAS", dto.documents().get(0).type());
    }

    @Test
    void toSummary_deberiaMapearElResumen() {
        var project = WebTestFixtures.completed(PROJECT_ID, 3, DocumentType.ROADMAP);

        var dto = mapper.toSummary(project);

        assertEquals(PROJECT_ID, dto.projectId());
        assertEquals(3, dto.version());
        assertEquals("COMPLETED", dto.status());
        assertEquals(1, dto.documentCount());
        assertEquals(project.updatedAt(), dto.updatedAt());
    }

    @Test
    void toVersion_deberiaMapearLaEntradaDeListado() {
        var project = WebTestFixtures.completed(PROJECT_ID, 1, DocumentType.KPI);

        var dto = mapper.toVersion(project);

        assertEquals(1, dto.version());
        assertEquals("COMPLETED", dto.status());
        assertEquals(project.createdAt(), dto.createdAt());
        assertEquals(project.updatedAt(), dto.updatedAt());
        assertEquals(project.completedAt(), dto.completedAt());
        assertNull(dto.failedReason());
    }

    @Test
    void toStatus_deberiaMapearElEstado() {
        var project = WebTestFixtures.completed(PROJECT_ID, 1, DocumentType.KPI);

        var dto = mapper.toStatus(project);

        assertEquals(PROJECT_ID, dto.projectId());
        assertEquals(1, dto.version());
        assertEquals("COMPLETED", dto.status());
        assertEquals(project.completedAt(), dto.completedAt());
    }

    @Test
    void toDocument_deberiaMapearLosMetadatos() {
        var artifact = WebTestFixtures.document(DocumentType.FINANCIAL_PLAN, 1);

        var dto = mapper.toDocument(artifact);

        assertEquals(artifact.id(), dto.id());
        assertEquals("FINANCIAL_PLAN", dto.type());
        assertEquals(artifact.size(), dto.size());
        assertEquals("BusinessModelEngine", dto.generatedBy());
        assertEquals("1.0.0", dto.engineVersion());
        assertEquals(1, dto.version());
        assertEquals("PDF", dto.renderFormat());
        assertEquals("text/plain", dto.mimeType());
        assertEquals("checksum-FINANCIAL_PLAN", dto.checksum());
        assertEquals("hash-FINANCIAL_PLAN", dto.inputHash());
    }

    @Test
    void toDocuments_deberiaMapearLaColeccion() {
        var docs = List.of(
            WebTestFixtures.document(DocumentType.LEAN_CANVAS, 1),
            WebTestFixtures.document(DocumentType.KPI, 1));

        var dtos = mapper.toDocuments(docs);

        assertEquals(2, dtos.size());
        assertEquals("LEAN_CANVAS", dtos.get(0).type());
        assertEquals("KPI", dtos.get(1).type());
    }

    @Test
    void toExport_deberiaMapearLosTamanoPorFormato() {
        var bundle = EnterpriseDocumentBundle.of(PROJECT_ID, 1, Map.of(
            DocumentType.LEAN_CANVAS, Map.of(
                RenderFormat.PDF, new byte[10], RenderFormat.DOCX, new byte[20])));

        var dto = mapper.toExport(bundle);

        assertEquals(PROJECT_ID, dto.projectId());
        assertEquals(1, dto.version());
        assertEquals(10L, dto.documents().get("LEAN_CANVAS").get("PDF"));
        assertEquals(20L, dto.documents().get("LEAN_CANVAS").get("DOCX"));
    }

    @Test
    void toDomain_deberiaConstruirLaSolicitudDeDominio() {
        var request = new EnterpriseGenerateRequest(false, null);
        var context = EngineTestFixtures.contextWithAll();

        var domain = mapper.toDomain(PROJECT_ID, request, context);

        assertEquals(PROJECT_ID, domain.projectId());
        assertEquals(context, domain.context());
        assertTrue(domain.recommendations().isEmpty());
        assertTrue(domain.opportunities().isEmpty());
        assertTrue(domain.knowledge().isEmpty());
        assertTrue(domain.riskResult().isEmpty());
    }

    @Test
    void toResponse_conProyectoFallido_deberiaExponerElMotivo() {
        var now = java.time.OffsetDateTime.now();
        var project = EnterpriseProject.fail(PROJECT_ID, 1, now, now, "motivo de fallo",
            java.util.List.of());

        var dto = mapper.toResponse(project);

        assertEquals("FAILED", dto.status());
        assertEquals("motivo de fallo", dto.failedReason());
        assertNull(dto.completedAt());
        assertEquals(0, dto.documentCount());
    }

    @Test
    void metodos_conEntradasNulas_deberianLanzar() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toResponse(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toSummary(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toVersion(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toStatus(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toDocuments(null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toExport(null));
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDomain(null, new EnterpriseGenerateRequest(false, null),
                EngineTestFixtures.contextWithAll()));
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDomain(PROJECT_ID, null, EngineTestFixtures.contextWithAll()));
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDomain(PROJECT_ID, new EnterpriseGenerateRequest(false, null), null));
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDashboard(null, java.util.List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> mapper.toDashboard(WebTestFixtures.completed(PROJECT_ID, 1, DocumentType.KPI), null));
    }

    @Test
    void toDashboard_deberiaConsolidarLaVista() {
        var project = WebTestFixtures.completed(PROJECT_ID, 2, DocumentType.LEAN_CANVAS, DocumentType.KPI);
        var versions = java.util.List.of(
            WebTestFixtures.completed(PROJECT_ID, 1, DocumentType.ROADMAP),
            project);

        var dashboard = mapper.toDashboard(project, versions);

        assertEquals(PROJECT_ID, dashboard.projectId());
        assertEquals(2, dashboard.version());
        assertEquals("COMPLETED", dashboard.status());
        assertEquals(100, dashboard.progress());
        assertEquals(2, dashboard.documentCount());
        assertEquals(2, dashboard.versionsCount());
        assertEquals(project.completedAt(), dashboard.completedAt());
        assertEquals(2, dashboard.documents().size());
        assertEquals(2, dashboard.versions().size());
        assertNull(dashboard.score());
        assertEquals(2L, dashboard.statistics().get("documentCount"));
        assertEquals(2L, dashboard.statistics().get("versionsCount"));
        assertTrue(dashboard.statistics().get("totalBytes") > 0);
    }

    @Test
    void toDashboard_conProyectoRequested_deberiaReflejarProgresoInicial() {
        var project = com.kinplatform.kin.enterprise.aggregate.EnterpriseProject.request(PROJECT_ID, 1);

        var dashboard = mapper.toDashboard(project, java.util.List.of(project));

        assertEquals("REQUESTED", dashboard.status());
        assertEquals(5, dashboard.progress());
        assertEquals(0, dashboard.documentCount());
        assertEquals(1, dashboard.versionsCount());
    }

    @Test
    void toDashboard_conProyectoFallido_deberiaReflejarElMotivo() {
        var now = java.time.OffsetDateTime.now();
        var project = EnterpriseProject.fail(PROJECT_ID, 1, now, now, "motor falló",
            java.util.List.of());

        var dashboard = mapper.toDashboard(project, java.util.List.of(project));

        assertEquals("FAILED", dashboard.status());
        assertEquals(100, dashboard.progress());
        assertEquals("motor falló", dashboard.failedReason());
    }
}
