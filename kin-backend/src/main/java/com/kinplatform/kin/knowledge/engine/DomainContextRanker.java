package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.orchestrator.ContextRanker;
import com.kinplatform.kin.knowledge.orchestrator.RankedCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranking determinista de candidatos ya validados (integración física):
 * ordena por confianza derivada (OFICIAL > SECUNDARIA > NO VERIFICADA),
 * rechazados al final, preservando el orden relativo en empates (estable).
 *
 * <p>No valida y no consulta políticas: solo ordena. Complementa (no sustituye)
 * a la validación del {@link SourceValidator}.</p>
 */
public class DomainContextRanker implements ContextRanker {

    @Override
    public List<RankedCandidate> rank(List<RankedCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        var ranked = new ArrayList<>(candidates);
        ranked.sort((a, b) -> Integer.compare(weightOf(b.validation().trust()), weightOf(a.validation().trust())));
        return List.copyOf(ranked);
    }

    private static int weightOf(SourceTrust trust) {
        return switch (trust) {
            case OFFICIAL_PUBLIC -> 2;
            case SECONDARY -> 1;
            case UNVERIFIED -> 0;
        };
    }
}
