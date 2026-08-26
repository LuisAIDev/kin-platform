package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generador del informe de cobertura de síntomas por condición (ADR-028, fase
 * de consolidación).
 *
 * <p>Para cada condición del catálogo calcula cuántos síntomas tiene asociados
 * (presentes) y cuántos son necesarios para un buen diagnóstico. Permite a los
 * profesionales de la salud identificar condiciones con cobertura deficiente
 * para revisión/ampliación. Determinista y sin LLM.</p>
 */
@Service
public class CoverageReportService {

    private final TriageKnowledgeRepository knowledgeRepository;

    public CoverageReportService(TriageKnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Transactional(readOnly = true)
    public CoverageReport generate() {
        TriageCatalog catalog = knowledgeRepository.loadCatalog();
        var items = catalog.conditions().stream()
                .map(condition -> {
                    int present = catalog.relationsForCondition(condition.id()).size();
                    boolean insufficient = present < 2;
                    return new CoverageItem(
                            condition.id(),
                            condition.name(),
                            condition.icdCode(),
                            present,
                            condition.validationStatus().name(),
                            insufficient);
                })
                .sorted(Comparator.comparingInt(CoverageItem::symptomCount))
                .toList();
        long insufficient = items.stream().filter(CoverageItem::insufficient).count();
        return new CoverageReport(items.size(), items.size() - (int) insufficient, (int) insufficient, items);
    }

    public record CoverageReport(
            int totalConditions, int adequateConditions, int insufficientConditions, List<CoverageItem> items) {

        public CoverageReport {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record CoverageItem(
            UUID conditionId,
            String name,
            String icdCode,
            int symptomCount,
            String validationStatus,
            boolean insufficient) {}
}
