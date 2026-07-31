package com.kinplatform.kin.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registro central de motores de dominio, auto-descubierto mediante Spring.
 *
 * <p>Recibe una {@code List<DomainEngine>} inyectada por el contenedor y la
 * indexa por nombre. Agregar un motor nuevo NUNCA requiere modificar esta
 * clase: basta con registrar un nuevo bean que implemente {@link DomainEngine}.</p>
 *
 * <p>Servicio de dominio puro: stateless, sin Spring, determinista.</p>
 */
public class EngineRegistry {

    private final Map<String, DomainEngine<?, ?>> engines;

    public EngineRegistry(List<? extends DomainEngine<?, ?>> engines) {
        var indexed = new LinkedHashMap<String, DomainEngine<?, ?>>();
        for (var engine : engines) {
            indexed.put(engine.metadata().name(), engine);
        }
        this.engines = Map.copyOf(indexed);
    }

    public Optional<DomainEngine<?, ?>> find(String name) {
        return Optional.ofNullable(engines.get(name));
    }

    public boolean contains(String name) {
        return engines.containsKey(name);
    }

    public Set<String> names() {
        return engines.keySet();
    }

    public int size() {
        return engines.size();
    }

    /**
     * Todos los motores, ordenados por fase y prioridad (orden de ejecución
     * recomendado por el diseño de ejecución por prioridades).
     */
    public List<DomainEngine<?, ?>> allOrdered() {
        var list = new ArrayList<DomainEngine<?, ?>>(engines.values());
        list.sort(Comparator
            .comparingInt((DomainEngine<?, ?> e) -> e.metadata().phase().ordinal())
            .thenComparingInt(e -> e.metadata().priority()));
        return List.copyOf(list);
    }

    public List<DomainEngine<?, ?>> byPhase(EnginePhase phase) {
        return allOrdered().stream()
            .filter(e -> e.metadata().phase() == phase)
            .toList();
    }

    public List<DomainEngine<?, ?>> after(String engineName) {
        var ordered = allOrdered();
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).metadata().name().equals(engineName)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return List.of();
        }
        return ordered.subList(idx + 1, ordered.size());
    }
}
