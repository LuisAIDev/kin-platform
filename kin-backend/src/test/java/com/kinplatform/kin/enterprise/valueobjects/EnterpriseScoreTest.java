package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseScoreTest {

    @Test
    void empty_deberiaCrearScoreEnCeroConGradoCritical() {
        var score = EnterpriseScore.empty();

        assertEquals(0.0, score.market());
        assertEquals(0.0, score.innovation());
        assertEquals(0.0, score.viability());
        assertEquals(0.0, score.financial());
        assertEquals(0.0, score.risk());
        assertEquals(0.0, score.scalability());
        assertEquals(0.0, score.team());
        assertEquals(0.0, score.sustainability());
        assertEquals(0, score.overallScore());
        assertEquals(0.0, score.confidence());
        assertEquals(ScoreGrade.CRITICAL, score.grade());
    }

    @Test
    void calculate_deberiaGuardarLasOchoDimensiones() {
        var score = EnterpriseScore.calculate(10, 20, 30, 40, 50, 60, 70, 80, 0.75);

        assertEquals(10, score.market());
        assertEquals(20, score.innovation());
        assertEquals(30, score.viability());
        assertEquals(40, score.financial());
        assertEquals(50, score.risk());
        assertEquals(60, score.scalability());
        assertEquals(70, score.team());
        assertEquals(80, score.sustainability());
        assertEquals(0.75, score.confidence());
    }

    @Test
    void calculate_overallScoreDeberiaSerLaMediaRedondeada() {
        var score = EnterpriseScore.calculate(10, 20, 30, 40, 50, 60, 70, 80, 0.75);
        assertEquals(45, score.overallScore());

        var max = EnterpriseScore.calculate(100, 100, 100, 100, 100, 100, 100, 100, 1.0);
        assertEquals(100, max.overallScore());
        assertEquals(ScoreGrade.EXCELLENT, max.grade());

        var min = EnterpriseScore.calculate(0, 0, 0, 0, 0, 0, 0, 0, 0.0);
        assertEquals(0, min.overallScore());
        assertEquals(ScoreGrade.CRITICAL, min.grade());
    }

    @Test
    void calculate_mediaConDecimales_deberiaRedondearAlEnteroMasCercano() {
        var score = EnterpriseScore.calculate(90, 91, 90, 90, 90, 90, 90, 91, 0.5);
        assertEquals(90, score.overallScore());
    }

    @Test
    void calculate_conDimensionFueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseScore.calculate(-1, 0, 0, 0, 0, 0, 0, 0, 0.5));
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseScore.calculate(0, 101, 0, 0, 0, 0, 0, 0, 0.5));
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseScore.calculate(0, 0, 0, 0, 0, 0, 0, 0, 1.1));
        assertThrows(IllegalArgumentException.class,
            () -> EnterpriseScore.calculate(0, 0, 0, 0, 0, 0, 0, 0, -0.1));
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var a = EnterpriseScore.calculate(10, 20, 30, 40, 50, 60, 70, 80, 0.5);
        var b = EnterpriseScore.calculate(10, 20, 30, 40, 50, 60, 70, 80, 0.5);
        var c = EnterpriseScore.calculate(11, 20, 30, 40, 50, 60, 70, 80, 0.5);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "score");
        assertNotEquals(a, EnterpriseScore.empty());
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        String s = EnterpriseScore.empty().toString();
        assertNotNull(s);
        assertTrue(s.contains("overall"));
        assertTrue(s.contains("grade"));
        assertTrue(s.contains("confidence"));
    }
}
