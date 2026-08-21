package com.kinplatform.ai.knowledge.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Selección determinista de fuentes por categoría de proyecto (ADR-024): una
 * fuente sin categorías (contexto general) siempre se consulta; una fuente con
 * categorías solo se consulta cuando la categoría del proyecto coincide.
 */
class CategoryAwareCompositeKnowledgeSourceTest {

    private static KnowledgeSource source(String id, AtomicInteger calls) {
        return query -> {
            calls.incrementAndGet();
            return List.of(new KnowledgeCandidate(
                    "hecho de " + id,
                    id,
                    "src",
                    "https://datos.gob.test/" + id,
                    java.time.OffsetDateTime.now(),
                    "application/json",
                    Map.of()));
        };
    }

    private static KnowledgeQuery query(String category) {
        return KnowledgeQuery.from(new KnowledgeRequest(
                "café", java.util.Set.of(), List.of("mercado"), 5, Duration.ofDays(365), category));
    }

    @Test
    void sinCategoria_consultaTodasLasFuentes() {
        var general = new AtomicInteger();
        var agro = new AtomicInteger();
        var fintech = new AtomicInteger();
        var composite = new CategoryAwareCompositeKnowledgeSource(
                List.of(source("general", general), source("agro", agro), source("fin", fintech)),
                List.of(List.of(), List.of("AGROINDUSTRIA"), List.of("FINTECH")));

        var candidates = composite.fetch(query(""));

        assertThat(candidates).hasSize(3);
        assertThat(general.get()).isEqualTo(1);
        assertThat(agro.get()).isEqualTo(1);
        assertThat(fintech.get()).isEqualTo(1);
    }

    @Test
    void categoriaAgroindustria_consultaGeneralYAgro_peroNoFintech() {
        var general = new AtomicInteger();
        var agro = new AtomicInteger();
        var fintech = new AtomicInteger();
        var composite = new CategoryAwareCompositeKnowledgeSource(
                List.of(source("general", general), source("agro", agro), source("fin", fintech)),
                List.of(List.of(), List.of("AGROINDUSTRIA"), List.of("FINTECH")));

        var candidates = composite.fetch(query("AGROINDUSTRIA"));

        assertThat(candidates).hasSize(2);
        assertThat(general.get()).isEqualTo(1);
        assertThat(agro.get()).isEqualTo(1);
        assertThat(fintech.get()).isZero();
    }

    @Test
    void categoriaFintech_consultaGeneralYFintech() {
        var general = new AtomicInteger();
        var agro = new AtomicInteger();
        var fintech = new AtomicInteger();
        var composite = new CategoryAwareCompositeKnowledgeSource(
                List.of(source("general", general), source("agro", agro), source("fin", fintech)),
                List.of(List.of(), List.of("AGROINDUSTRIA"), List.of("FINTECH")));

        var candidates = composite.fetch(query("FINTECH"));

        assertThat(candidates).hasSize(2);
        assertThat(fintech.get()).isEqualTo(1);
        assertThat(agro.get()).isZero();
    }

    @Test
    void fuenteMulticategoria_sirveAVarias() {
        var multi = new AtomicInteger();
        var composite = new CategoryAwareCompositeKnowledgeSource(
                List.of(source("tasas", multi)), List.of(List.of("FINTECH", "EMPRESARIAL")));

        assertThat(composite.fetch(query("FINTECH"))).hasSize(1);
        assertThat(composite.fetch(query("EMPRESARIAL"))).hasSize(1);
        assertThat(composite.fetch(query("AGROINDUSTRIA"))).isEmpty();
        assertThat(multi.get()).isEqualTo(2);
    }

    @Test
    void categoriaInactiva_degradaConGracia() {
        var general = new AtomicInteger();
        var composite =
                new CategoryAwareCompositeKnowledgeSource(List.of(source("general", general)), List.of(List.of()));

        assertThat(composite.fetch(query("SALUD"))).hasSize(1);
        assertThat(general.get()).isEqualTo(1);
    }

    @Test
    void fuenteDeshabilitada_noSeConsulta() {
        var disabled = new AtomicInteger();
        var general = new AtomicInteger();
        var composite = new CategoryAwareCompositeKnowledgeSource(
                List.of(source("deshabilitada", disabled), source("general", general)),
                List.of(List.of(), List.of()),
                List.of(false, true),
                List.of(0, 0));

        assertThat(composite.fetch(query("AGROINDUSTRIA"))).hasSize(1);
        assertThat(disabled.get()).isZero();
        assertThat(general.get()).isEqualTo(1);
    }

    @Test
    void prioridad_ordenaLasFuentesDescendente() {
        var calls = new java.util.ArrayList<String>();
        KnowledgeSource low = q -> {
            calls.add("low");
            return List.of(new KnowledgeCandidate(
                    "low",
                    "low",
                    "src",
                    "https://datos.gob.test/low",
                    java.time.OffsetDateTime.now(),
                    "application/json",
                    Map.of()));
        };
        KnowledgeSource high = q -> {
            calls.add("high");
            return List.of(new KnowledgeCandidate(
                    "high",
                    "high",
                    "src",
                    "https://datos.gob.test/high",
                    java.time.OffsetDateTime.now(),
                    "application/json",
                    Map.of()));
        };
        var composite = new CategoryAwareCompositeKnowledgeSource(
                List.of(low, high), List.of(List.of(), List.of()), List.of(true, true), List.of(1, 10));

        assertThat(composite.fetch(query(""))).hasSize(2);
        assertThat(calls).containsExactly("high", "low");
    }
}
