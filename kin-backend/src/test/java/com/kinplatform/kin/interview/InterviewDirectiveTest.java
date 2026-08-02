package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewDirectiveTest {

    @Test
    void of_deberiaExponerCampos() {
        var rules = AnswerRules.of(5, true, 1);
        var directive = InterviewDirective.of("q1", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", rules);

        assertEquals("q1", directive.questionId());
        assertEquals(AnalyzedDimension.REVENUE_MODEL, directive.dimension());
        assertEquals("modelo de ingresos", directive.topic());
        assertEquals(rules, directive.rules());
    }

    @Test
    void of_deberiaUsarReglasPorDefectoSiFaltan() {
        var directive = InterviewDirective.of("q1", AnalyzedDimension.CITY, "ubicación", null);

        assertEquals(AnswerRules.defaults(), directive.rules());
    }

    @Test
    void constructor_deberiaValidarIdentidad() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewDirective(null, AnalyzedDimension.CITY, "t", null));
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewDirective("q1", null, "t", null));
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewDirective("q1", AnalyzedDimension.CITY, "", null));
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewDirective("q1", AnalyzedDimension.CITY, " ", null));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewDirective.of("q1", AnalyzedDimension.CITY, "ubicación", null);
        var b = InterviewDirective.of("q1", AnalyzedDimension.CITY, "ubicación", null);
        var c = InterviewDirective.of("q2", AnalyzedDimension.CITY, "ubicación", null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("questionId=q1"));
    }
}
