package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.reporting.report.assembler.ReportMetadataAssembler;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ReportBuilder;

/**
 * Orquestador puro del {@link ConsultingReport}.
 *
 * <p>Recibe los resultados YA calculados por el pipeline en {@link ReportInput}
 * y coordina los 11 {@link SectionAssembler} tipados (10 históricos + el
 * aditivo {@code sources}) para ensamblar el reporte con {@link ReportBuilder}.
 * No invoca motores, no recalcula scores,
 * prioridades, confianzas ni niveles. Servicio de dominio puro: stateless,
 * determinista y sin infraestructura.</p>
 */
public class ReportEngine implements DomainEngine<ReportInput, ConsultingReport> {

    public static final String GENERATOR_NAME = ReportMetadataAssembler.GENERATOR_NAME;

    private final ReportAssemblers assemblers;
    private final ReportModel model;

    public ReportEngine(ReportAssemblers assemblers, ReportModel model) {
        this.assemblers = assemblers;
        this.model = model;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, model.version(), "KIN Architecture Team",
            EnginePhase.REPORTING, EngineType.DOMAIN, 70);
    }

    @Override
    public ConsultingReport evaluate(ReportInput input) {
        if (input == null || input.projectContext() == null
            || input.evaluation() == null || input.score() == null) {
            return ConsultingReport.empty();
        }
        return ReportBuilder.create(input.projectId())
            .executiveSummary(assemblers.executiveSummary().assemble(input))
            .scores(assemblers.scores().assemble(input))
            .recommendations(assemblers.recommendations().assemble(input))
            .risks(assemblers.risks().assemble(input))
            .opportunities(assemblers.opportunities().assemble(input))
            .financial(assemblers.financial().assemble(input))
            .market(assemblers.market().assemble(input))
            .innovation(assemblers.innovation().assemble(input))
            .nextSteps(assemblers.nextSteps().assemble(input))
            .sources(assemblers.sources().assemble(input))
            .metadata(assemblers.metadata().assemble(input))
            .build();
    }

    public ReportAssemblers assemblers() {
        return assemblers;
    }
}
