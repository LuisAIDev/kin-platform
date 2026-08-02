package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewQuestionTest {

    @Test
    void required_deberiaSerObligatoriaConReglasPorDefecto() {
        var question = InterviewQuestion.required("q1", AnalyzedDimension.REVENUE_MODEL, "modelo de ingresos", 1);

        assertEquals("q1", question.id());
        assertEquals(AnalyzedDimension.REVENUE_MODEL, question.dimension());
        assertEquals("modelo de ingresos", question.topic());
        assertTrue(question.required());
        assertEquals(1, question.order());
        assertEquals(AnswerRules.defaults(), question.rules());
        assertTrue(question.followUpIds().isEmpty());
        assertFalse(question.hasFollowUps());
    }

    @Test
    void optional_deberiaSerOpcional() {
        var question = InterviewQuestion.optional("q2", AnalyzedDimension.CITY, "ubicación", 2);

        assertFalse(question.required());
        assertFalse(question.hasFollowUps());
    }

    @Test
    void constructor_deberiaExponerCamposCompletos() {
        var rules = AnswerRules.of(5, true, 1);
        var question = new InterviewQuestion(
            "q1", AnalyzedDimension.TARGET_CUSTOMER, "cliente objetivo", true, 3, rules,
            List.of("q1a", "q1b"));

        assertEquals(List.of("q1a", "q1b"), question.followUpIds());
        assertEquals(rules, question.rules());
        assertTrue(question.hasFollowUps());
    }

    @Test
    void constructor_deberiaValidarIdentidad() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewQuestion(null, AnalyzedDimension.CITY, "t", true, 0, null, null));
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewQuestion(" ", AnalyzedDimension.CITY, "t", true, 0, null, null));
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewQuestion("q1", null, "t", true, 0, null, null));
        assertThrows(IllegalArgumentException.class,
            () -> new InterviewQuestion("q1", AnalyzedDimension.CITY, "", true, 0, null, null));
    }

    @Test
    void constructor_deberiaAcotarYNormalizar() {
        var question = new InterviewQuestion("q1", AnalyzedDimension.CITY, "ubicación", true, -2, null, null);

        assertEquals(0, question.order());
        assertEquals(AnswerRules.defaults(), question.rules());
        assertTrue(question.followUpIds().isEmpty());
    }

    @Test
    void constructor_deberiaProtegerFollowUps() {
        var followUps = new ArrayList<>(List.of("q1a"));
        var question = new InterviewQuestion("q1", AnalyzedDimension.CITY, "ubicación", true, 0, null, followUps);

        followUps.clear();
        assertEquals(1, question.followUpIds().size());
        assertThrows(UnsupportedOperationException.class,
            () -> question.followUpIds().add("x"));
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewQuestion.required("q1", AnalyzedDimension.CITY, "ubicación", 1);
        var b = InterviewQuestion.required("q1", AnalyzedDimension.CITY, "ubicación", 1);
        var c = InterviewQuestion.required("q2", AnalyzedDimension.CITY, "ubicación", 1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("id=q1"));
    }
}
