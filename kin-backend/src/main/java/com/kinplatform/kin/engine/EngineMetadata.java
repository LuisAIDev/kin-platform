package com.kinplatform.kin.engine;

import java.util.List;

/**
 * Metadatos declarativos de un motor de dominio. Inmutable.
 *
 * <p>Cada motor informa su identidad, fase, tipo, prioridad y dependencias
 * para que el {@link EngineRegistry} lo descubra automáticamente y el
 * {@link EngineExecutor} lo ejecute de forma ordenada sin conocer su
 * implementación.</p>
 */
public record EngineMetadata(
    String name,
    String version,
    String author,
    EnginePhase phase,
    EngineType type,
    int priority,
    List<String> dependencies
) {

    public EngineMetadata {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    public static EngineMetadata of(String name, String version, String author,
                                    EnginePhase phase, EngineType type, int priority) {
        return new EngineMetadata(name, version, author, phase, type, priority, List.of());
    }

    public boolean dependsOn(String engineName) {
        return dependencies.contains(engineName);
    }
}
