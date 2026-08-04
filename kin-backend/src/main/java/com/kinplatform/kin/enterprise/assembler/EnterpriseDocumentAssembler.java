package com.kinplatform.kin.enterprise.assembler;

import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.enterprise.engine.result.BusinessModelResult;
import com.kinplatform.kin.enterprise.engine.result.FinancialPlanResult;
import com.kinplatform.kin.enterprise.engine.result.InnovationResult;
import com.kinplatform.kin.enterprise.engine.result.KpiResult;
import com.kinplatform.kin.enterprise.engine.result.MarketResult;
import com.kinplatform.kin.enterprise.engine.result.RiskPlanResult;
import com.kinplatform.kin.enterprise.engine.result.RoadmapResult;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ensamblador de documentos del proyecto empresarial (Fase 10, Milestone 2E).
 *
 * <p>Transforma los value objects producidos por los motores deterministas en
 * {@link DocumentArtifact} listos para adjuntarse al aggregate
 * {@code EnterpriseProject}. Cada resultado de motor no vacío produce un
 * documento del catálogo {@link DocumentType}: Lean Canvas, plan de mercado,
 * plan de innovación, plan financiero, hoja de ruta, matriz de riesgos y
 * conjunto de KPIs.</p>
 *
 * <p>El contenido es una representación neutral determinista (serialización
 * por componentes del value object) sin formato de salida: el renderizado a
 * PDF/DOCX/PPTX pertenece a los {@code DocumentRenderer} del Milestone
 * posterior. El documento {@code DOFA} (composición entre modelos) y el
 * {@code EXECUTIVE_REPORT} (narrativa con LLM) se ensamblan en milestones
 * posteriores y no forman parte de este paso.</p>
 *
 * <p>Los resultados vacíos o nulos se omiten: un proyecto siempre obtiene un
 * subconjunto de documentos acorde a la información realmente producida
 * (regla funcional: nunca inventar documentos).</p>
 */
public final class EnterpriseDocumentAssembler {

    /** Prefijo del hash determinista de la entrada de cada documento. */
    private static final String HASH_PREFIX = "sha256";

    /**
     * Ensambla los documentos de la versión a partir de los resultados de los
     * motores. Solo se incluyen los resultados no vacíos.
     *
     * @param version      versión del proyecto empresarial a la que pertenecen
     * @param businessModel resultado del motor de modelo de negocio
     * @param market        resultado del motor de mercado
     * @param innovation    resultado del motor de innovación
     * @param financialPlan resultado del motor financiero
     * @param roadmap       resultado del motor de hoja de ruta
     * @param riskPlan      resultado del motor de matriz de riesgos
     * @param kpi           resultado del motor de KPIs
     * @return lista inmutable (posiblemente vacía) de artefactos de documento
     */
    public List<DocumentArtifact> assemble(int version,
                                           BusinessModelResult businessModel,
                                           MarketResult market,
                                           InnovationResult innovation,
                                           FinancialPlanResult financialPlan,
                                           RoadmapResult roadmap,
                                           RiskPlanResult riskPlan,
                                           KpiResult kpi) {
        var artifacts = new ArrayList<DocumentArtifact>();
        add(artifacts, version, DocumentType.LEAN_CANVAS, businessModel,
            businessModel == null ? null : businessModel.canvas());
        add(artifacts, version, DocumentType.MARKET_PLAN, market,
            market == null ? null : market.plan());
        add(artifacts, version, DocumentType.INNOVATION_PLAN, innovation,
            innovation == null ? null : innovation.plan());
        add(artifacts, version, DocumentType.FINANCIAL_PLAN, financialPlan,
            financialPlan == null ? null : financialPlan.plan());
        add(artifacts, version, DocumentType.ROADMAP, roadmap,
            roadmap == null ? null : roadmap.roadmap());
        add(artifacts, version, DocumentType.RISK_MATRIX, riskPlan,
            riskPlan == null ? null : riskPlan.matrix());
        add(artifacts, version, DocumentType.KPI, kpi,
            kpi == null ? null : kpi.kpis());
        return List.copyOf(artifacts);
    }

    private void add(List<DocumentArtifact> artifacts, int version, DocumentType type,
                     EngineResult result, Object value) {
        if (result == null || result.isEmpty() || value == null) {
            return;
        }
        String content = render(value);
        artifacts.add(new DocumentArtifact(
            UUID.randomUUID(),
            type,
            content,
            OffsetDateTime.now(),
            result.generatedBy(),
            result.engineVersion(),
            inputHash(content),
            version));
    }

    /**
     * Crea un documento narrativo (Fase 10, Milestone 3E): artefacto generado
     * por la IA (o su fallback determinista) para los tipos sin value object
     * propio ({@code EXECUTIVE_REPORT} y {@code DOFA}). Reutiliza el hash de
     * entrada y la construcción común de {@link DocumentArtifact}.
     *
     * @param version       versión del proyecto empresarial a la que pertenece
     * @param type          tipo de documento narrativo (no nulo)
     * @param content       contenido narrativo (no nulo ni en blanco)
     * @param generatedBy   motor que generó el documento (obligatorio)
     * @param engineVersion versión del motor (obligatorio)
     * @return artefacto de documento narrativo
     * @throws IllegalArgumentException si el contenido es nulo o en blanco
     */
    public DocumentArtifact narrative(int version, DocumentType type, String content,
                                      String generatedBy, String engineVersion) {
        if (type == null) {
            throw new IllegalArgumentException("type no puede ser null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("'content' narrativo no puede ser nulo o en blanco.");
        }
        return new DocumentArtifact(
            UUID.randomUUID(),
            type,
            content,
            OffsetDateTime.now(),
            generatedBy,
            engineVersion,
            inputHash(content),
            version);
    }

    /**
     * Serialización determinista del value object a representación neutral
     * (reutilizable por el generador narrativo para alimentar el prompt de la
     * IA con los mismos datos que los documentos deterministas).
     *
     * <p>Recorre los componentes del record en orden de declaración y produce
     * líneas {@code nombre: valor}. Las listas se serializan como ítems
     * {@code - valor} y los records anidados se serializan recursivamente. La
     * salida depende únicamente de los datos del value object: misma entrada,
     * mismo contenido.</p>
     */
    public String render(Object value) {
        var sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }

    private void append(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String text) {
            sb.append(text);
            return;
        }
        if (value instanceof Enum<?> constant) {
            sb.append(constant.name());
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            appendIterable(sb, iterable);
            return;
        }
        if (value.getClass().isRecord()) {
            appendRecord(sb, value);
            return;
        }
        sb.append(value);
    }

    private void appendIterable(StringBuilder sb, Iterable<?> iterable) {
        boolean first = true;
        for (Object item : iterable) {
            if (!first) {
                sb.append('\n');
            }
            sb.append("- ");
            append(sb, item);
            first = false;
        }
        if (first) {
            sb.append("[]");
        }
    }

    private void appendRecord(StringBuilder sb, Object record) {
        var components = record.getClass().getRecordComponents();
        boolean first = true;
        for (var component : components) {
            if (!first) {
                sb.append('\n');
            }
            sb.append(component.getName()).append(": ");
            try {
                append(sb, component.getAccessor().invoke(record));
            } catch (ReflectiveOperationException ex) {
                sb.append("?");
            }
            first = false;
        }
    }

    private String inputHash(String content) {
        return HASH_PREFIX + ":" + Integer.toUnsignedString(content.hashCode(), 16);
    }
}
