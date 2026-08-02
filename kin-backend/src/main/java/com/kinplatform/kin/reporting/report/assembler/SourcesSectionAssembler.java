package com.kinplatform.kin.reporting.report.assembler;

import com.kinplatform.kin.enrichment.EnrichmentResult;
import com.kinplatform.kin.reporting.report.ReportInput;
import com.kinplatform.kin.reporting.report.SectionAssembler;
import com.kinplatform.kin.reporting.report.model.CitedSource;
import com.kinplatform.kin.reporting.report.model.SourcesSection;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Ensambla la sección de fuentes citadas (ADR-016, Etapa E5): transforma el
 * {@link EnrichmentResult} portado por el {@link ReportInput} en una
 * {@link SourcesSection}, deduplicando por {@code sourceId} y conservando la
 * evidencia de mayor score.
 *
 * <p>Aditivo: sin enriquecimiento ({@code null} o vacío) devuelve
 * {@link SourcesSection#empty()} y el reporte se comporta exactamente como
 * antes de la Fase 8.</p>
 */
public class SourcesSectionAssembler implements SectionAssembler<SourcesSection> {

    @Override
    public SourcesSection assemble(ReportInput input) {
        EnrichmentResult enrichment = input.enrichment();
        if (enrichment == null || enrichment.isEmpty()) {
            return SourcesSection.empty();
        }
        var bySourceId = new LinkedHashMap<String, CitedSource>();
        for (var rank : enrichment.ranks()) {
            for (var evidence : rank.evidence()) {
                var fact = evidence.fact();
                if (fact == null || fact.sourceId() == null || fact.sourceId().isBlank()) {
                    continue;
                }
                var current = bySourceId.get(fact.sourceId());
                if (current == null || evidence.scoreValue() > current.score()) {
                    bySourceId.put(fact.sourceId(), new CitedSource(
                        fact.sourceId(),
                        fact.url(),
                        fact.claim(),
                        rank.category(),
                        evidence.scoreValue()));
                }
            }
        }
        return new SourcesSection(List.copyOf(bySourceId.values()));
    }
}
