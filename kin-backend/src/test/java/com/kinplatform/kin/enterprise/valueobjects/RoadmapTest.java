package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapTest {

    // ------------------------------------------------------------------
    // GanttEntry
    // ------------------------------------------------------------------

    @Test
    void ganttEntry_deberiaGuardarValores() {
        var entry = Roadmap.GanttEntry.of("Desarrollo", 1, 6);

        assertEquals("Desarrollo", entry.task());
        assertEquals(1, entry.startMonth());
        assertEquals(6, entry.endMonth());
    }

    @Test
    void ganttEntry_conTaskEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> Roadmap.GanttEntry.of("", 1, 2));
    }

    @Test
    void ganttEntry_conStartMonthNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> Roadmap.GanttEntry.of("T", -1, 2));
    }

    @Test
    void ganttEntry_conEndMonthAnteriorAlStart_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> Roadmap.GanttEntry.of("T", 3, 2));
    }

    @Test
    void ganttEntry_conEndMonthIgualAlStart_deberiaAceptarse() {
        var entry = Roadmap.GanttEntry.of("T", 2, 2);
        assertEquals(2, entry.endMonth());
    }

    @Test
    void ganttEntry_equals_deberiaCompararPorValor() {
        assertEquals(Roadmap.GanttEntry.of("T", 1, 2), Roadmap.GanttEntry.of("T", 1, 2));
        assertNotEquals(Roadmap.GanttEntry.of("T", 1, 2), Roadmap.GanttEntry.of("T", 1, 3));
    }

    // ------------------------------------------------------------------
    // Roadmap
    // ------------------------------------------------------------------

    @Test
    void empty_deberiaCrearRoadmapVacio() {
        var roadmap = Roadmap.empty();

        assertTrue(roadmap.phases().isEmpty());
        assertTrue(roadmap.milestones().isEmpty());
        assertEquals("Sin definir", roadmap.timeline());
        assertTrue(roadmap.dependencies().isEmpty());
        assertTrue(roadmap.ganttEntries().isEmpty());
    }

    @Test
    void of_deberiaGuardarTodosLosCampos() {
        var entry = Roadmap.GanttEntry.of("Desarrollo", 1, 6);
        var roadmap = Roadmap.of(List.of("Fase 1"), List.of("Hito A"), "12 meses",
            List.of("Dependencia 1"), List.of(entry));

        assertEquals(List.of("Fase 1"), roadmap.phases());
        assertEquals(List.of("Hito A"), roadmap.milestones());
        assertEquals("12 meses", roadmap.timeline());
        assertEquals(List.of("Dependencia 1"), roadmap.dependencies());
        assertEquals(List.of(entry), roadmap.ganttEntries());
    }

    @Test
    void of_conTimelineEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> Roadmap.of(List.of(), List.of(), "  ", List.of(), List.of()));
    }

    @Test
    void of_conFasesConBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> Roadmap.of(List.of(""), List.of(), "T", List.of(), List.of()));
    }

    @Test
    void of_conGanttConNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> Roadmap.of(List.of(), List.of(), "T", List.of(),
                java.util.Arrays.asList((Roadmap.GanttEntry) null)));
    }

    @Test
    void ganttEntries_deberianSerInmutables() {
        var roadmap = Roadmap.of(List.of(), List.of(), "T", List.of(),
            List.of(Roadmap.GanttEntry.of("T", 1, 2)));
        assertThrows(UnsupportedOperationException.class,
            () -> roadmap.ganttEntries().add(Roadmap.GanttEntry.of("U", 1, 2)));
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var a = Roadmap.of(List.of("F"), List.of(), "T", List.of(), List.of());
        var b = Roadmap.of(List.of("F"), List.of(), "T", List.of(), List.of());
        var c = Roadmap.of(List.of("G"), List.of(), "T", List.of(), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "roadmap");
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        assertNotNull(Roadmap.empty().toString());
        assertTrue(Roadmap.empty().toString().contains("timeline"));
    }
}
