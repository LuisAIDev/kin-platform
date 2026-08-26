package com.kinplatform.kin.health.triage;

import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación en memoria del puerto {@link TriageKnowledgeRepository} para
 * tests unitarios y de integración (ADR-028): permite verificar el motor, el
 * stage y la actualización del catálogo sin infraestructura JPA.
 */
public class InMemoryTriageKnowledgeRepository implements TriageKnowledgeRepository {

    private TriageCatalog catalog;

    public InMemoryTriageKnowledgeRepository(TriageCatalog catalog) {
        this.catalog = catalog == null ? TriageCatalog.empty() : catalog;
    }

    @Override
    public TriageCatalog loadCatalog() {
        return catalog;
    }

    @Override
    public CatalogUpdateResult applyUpdate(CatalogUpdate update) {
        if (update == null || update.isEmpty()) {
            return CatalogUpdateResult.empty(update == null ? "" : update.source());
        }
        int conditionsAdded = 0;
        int symptomsAdded = 0;
        int relationsAdded = 0;
        var symptoms = new ArrayList<>(catalog.symptoms());
        var conditions = new ArrayList<>(catalog.conditions());
        var relations = new ArrayList<>(catalog.relations());
        for (var s : update.symptoms()) {
            boolean exists = symptoms.stream()
                    .anyMatch(e -> e.id().equals(s.id()) || e.name().equalsIgnoreCase(s.name()));
            if (!exists) {
                symptoms.add(s);
                symptomsAdded++;
            }
        }
        for (var c : update.conditions()) {
            boolean exists = conditions.stream()
                    .anyMatch(e -> e.id().equals(c.id()) || e.name().equalsIgnoreCase(c.name()));
            if (!exists) {
                conditions.add(c);
                conditionsAdded++;
            }
        }
        for (var r : update.relations()) {
            boolean exists = relations.stream()
                    .anyMatch(e -> e.symptomId().equals(r.symptomId())
                            && e.conditionId().equals(r.conditionId()));
            if (!exists) {
                relations.add(r);
                relationsAdded++;
            }
        }
        catalog = new TriageCatalog(List.copyOf(symptoms), List.copyOf(conditions), List.copyOf(relations));
        return CatalogUpdateResult.of(symptomsAdded, conditionsAdded, relationsAdded, update.source());
    }
}
