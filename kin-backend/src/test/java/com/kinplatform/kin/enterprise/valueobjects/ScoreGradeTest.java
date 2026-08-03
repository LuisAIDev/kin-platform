package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreGradeTest {

    @Test
    void from_con90Omas_deberiaSerExcellent() {
        assertEquals(ScoreGrade.EXCELLENT, ScoreGrade.from(90));
        assertEquals(ScoreGrade.EXCELLENT, ScoreGrade.from(95));
        assertEquals(ScoreGrade.EXCELLENT, ScoreGrade.from(100));
    }

    @Test
    void from_con75A89_deberiaSerGood() {
        assertEquals(ScoreGrade.GOOD, ScoreGrade.from(75));
        assertEquals(ScoreGrade.GOOD, ScoreGrade.from(80));
        assertEquals(ScoreGrade.GOOD, ScoreGrade.from(89));
    }

    @Test
    void from_con60A74_deberiaSerFair() {
        assertEquals(ScoreGrade.FAIR, ScoreGrade.from(60));
        assertEquals(ScoreGrade.FAIR, ScoreGrade.from(65));
        assertEquals(ScoreGrade.FAIR, ScoreGrade.from(74));
    }

    @Test
    void from_con40A59_deberiaSerWeak() {
        assertEquals(ScoreGrade.WEAK, ScoreGrade.from(40));
        assertEquals(ScoreGrade.WEAK, ScoreGrade.from(50));
        assertEquals(ScoreGrade.WEAK, ScoreGrade.from(59));
    }

    @Test
    void from_conMenosDe40_deberiaSerCritical() {
        assertEquals(ScoreGrade.CRITICAL, ScoreGrade.from(39));
        assertEquals(ScoreGrade.CRITICAL, ScoreGrade.from(20));
        assertEquals(ScoreGrade.CRITICAL, ScoreGrade.from(0));
    }
}
