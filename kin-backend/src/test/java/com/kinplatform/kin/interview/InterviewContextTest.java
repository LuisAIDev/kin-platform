package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewContextTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Test
    void of_deberiaExponerCampos() {
        var covered = Set.of(AnalyzedDimension.SECTOR, AnalyzedDimension.SOLUTION);
        var context = InterviewContext.of(PROJECT_ID, "Mi Proyecto", "Software", covered);

        assertEquals(PROJECT_ID, context.projectId());
        assertEquals("Mi Proyecto", context.projectTitle());
        assertEquals("Software", context.projectCategory());
        assertEquals(covered, context.coveredDimensions());
        assertTrue(context.covers(AnalyzedDimension.SECTOR));
        assertFalse(context.covers(AnalyzedDimension.RISKS));
    }

    @Test
    void ofProject_deberiaCrearContextoMinimo() {
        var context = InterviewContext.ofProject(PROJECT_ID);

        assertEquals(PROJECT_ID, context.projectId());
        assertEquals("", context.projectTitle());
        assertEquals("", context.projectCategory());
        assertTrue(context.coveredDimensions().isEmpty());
    }

    @Test
    void coverageRatio_deberiaCalcularCobertura() {
        var context = InterviewContext.of(PROJECT_ID, "t", "c",
            Set.of(AnalyzedDimension.SECTOR, AnalyzedDimension.SOLUTION));

        assertEquals(2.0 / AnalyzedDimension.values().length, context.coverageRatio(), 0.0001);

        assertEquals(0.0, InterviewContext.ofProject(PROJECT_ID).coverageRatio(), 0.0001);
    }

    @Test
    void constructor_deberiaValidarProjectId() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewContext(null, "t", "c", Set.of()));
    }

    @Test
    void constructor_deberiaNormalizarNulos() {
        var context = new InterviewContext(PROJECT_ID, null, null, null);

        assertEquals("", context.projectTitle());
        assertEquals("", context.projectCategory());
        assertTrue(context.coveredDimensions().isEmpty());
    }

    @Test
    void constructor_deberiaProtegerSetDeDimensiones() {
        var covered = new HashSet<>(Set.of(AnalyzedDimension.SECTOR));
        var context = new InterviewContext(PROJECT_ID, "t", "c", covered);

        covered.clear();
        assertEquals(1, context.coveredDimensions().size());
        assertThrows(UnsupportedOperationException.class,
            () -> context.coveredDimensions().add(AnalyzedDimension.RISKS));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var covered = Set.of(AnalyzedDimension.SECTOR);
        var a = InterviewContext.of(PROJECT_ID, "t", "c", covered);
        var b = InterviewContext.of(PROJECT_ID, "t", "c", covered);
        var c = InterviewContext.ofProject(PROJECT_ID);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("projectId"));
    }
}
