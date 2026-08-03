package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Hoja de ruta del proyecto empresarial (value object).
 *
 * <p>Representa la planificación temporal: fases, hitos, descripción del
 * cronograma, dependencias y entradas de diagrama de Gantt
 * ({@link GanttEntry}). Producido por {@code RoadmapEngine}.</p>
 */
public record Roadmap(
    List<String> phases,
    List<String> milestones,
    String timeline,
    List<String> dependencies,
    List<GanttEntry> ganttEntries
) {

    public Roadmap {
        phases = ValueObjects.immutableNotBlank(phases, "phases");
        milestones = ValueObjects.immutableNotBlank(milestones, "milestones");
        ValueObjects.requireNotBlank(timeline, "timeline");
        dependencies = ValueObjects.immutableNotBlank(dependencies, "dependencies");
        ganttEntries = ValueObjects.immutableNonNull(ganttEntries, "ganttEntries");
    }

    /**
     * Entrada de diagrama de Gantt (value object): tarea con mes de inicio y
     * mes de fin. El mes de fin no puede ser anterior al mes de inicio.
     *
     * @param task       nombre de la tarea (no vacío)
     * @param startMonth mes de inicio (1-indexado, mayor o igual a 1)
     * @param endMonth   mes de fin (mayor o igual a {@code startMonth})
     */
    public record GanttEntry(String task, int startMonth, int endMonth) {

        public GanttEntry {
            ValueObjects.requireNotBlank(task, "task");
            ValueObjects.requireNonNegative(startMonth, "startMonth");
            if (endMonth < startMonth) {
                throw new IllegalArgumentException("'endMonth' no puede ser anterior a 'startMonth'.");
            }
        }

        public static GanttEntry of(String task, int startMonth, int endMonth) {
            return new GanttEntry(task, startMonth, endMonth);
        }
    }

    /**
     * Crea una hoja de ruta vacía (fases, hitos y dependencias vacíos).
     */
    public static Roadmap empty() {
        return new Roadmap(List.of(), List.of(), "Sin definir", List.of(), List.of());
    }

    /**
     * Crea una hoja de ruta completa.
     */
    public static Roadmap of(List<String> phases, List<String> milestones, String timeline,
                             List<String> dependencies, List<GanttEntry> ganttEntries) {
        return new Roadmap(phases, milestones, timeline, dependencies, ganttEntries);
    }
}
