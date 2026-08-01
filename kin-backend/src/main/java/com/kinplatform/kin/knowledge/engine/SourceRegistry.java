package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registro de fuentes de conocimiento disponibles (ADR-014, §5.2).
 *
 * <p>Administra únicamente los {@link KnowledgeSource} registrados
 * (auto-descubrimiento por inyección de {@code List<KnowledgeSource>},
 * patrón {@code EngineRegistry}). Servicio de dominio puro: sin reflection,
 * sin Spring, sin infraestructura.</p>
 *
 * <p>Preserva el orden de registro y evita duplicados; {@link #all()} devuelve
 * una vista inmutable.</p>
 */
public class SourceRegistry {

    private final Set<KnowledgeSource> sources;

    public SourceRegistry(List<KnowledgeSource> sources) {
        this.sources = new LinkedHashSet<>();
        if (sources != null) {
            for (var source : sources) {
                if (source != null) {
                    this.sources.add(source);
                }
            }
        }
    }

    public static SourceRegistry empty() {
        return new SourceRegistry(List.of());
    }

    public void register(KnowledgeSource source) {
        if (source != null) {
            this.sources.add(source);
        }
    }

    /**
     * Todas las fuentes registradas, en orden de registro e inmodificables.
     */
    public List<KnowledgeSource> all() {
        return List.copyOf(this.sources);
    }

    public int size() {
        return this.sources.size();
    }

    public boolean isEmpty() {
        return this.sources.isEmpty();
    }
}
