package com.kinplatform.kin.enterprise.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskMatrixTest {

    // ------------------------------------------------------------------
    // Risk
    // ------------------------------------------------------------------

    @Test
    void risk_deberiaGuardarValores() {
        var risk = RiskMatrix.Risk.of(0.4, 0.5, RiskSeverity.HIGH, "Mitigar", "Juan",
            RiskStatus.MITIGATING);

        assertEquals(0.4, risk.probability());
        assertEquals(0.5, risk.impact());
        assertEquals(RiskSeverity.HIGH, risk.severity());
        assertEquals("Mitigar", risk.mitigation());
        assertEquals("Juan", risk.owner());
        assertEquals(RiskStatus.MITIGATING, risk.status());
    }

    @Test
    void risk_conProbabilidadFueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(-0.1, 0.5, RiskSeverity.LOW, "M", "O", RiskStatus.IDENTIFIED));
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(1.1, 0.5, RiskSeverity.LOW, "M", "O", RiskStatus.IDENTIFIED));
    }

    @Test
    void risk_conImpactoFueraDeRango_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(0.5, -0.1, RiskSeverity.LOW, "M", "O", RiskStatus.IDENTIFIED));
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(0.5, 1.1, RiskSeverity.LOW, "M", "O", RiskStatus.IDENTIFIED));
    }

    @Test
    void risk_conSeverityNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(0.5, 0.5, null, "M", "O", RiskStatus.IDENTIFIED));
    }

    @Test
    void risk_conMitigationEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(0.5, 0.5, RiskSeverity.LOW, "", "O", RiskStatus.IDENTIFIED));
    }

    @Test
    void risk_conOwnerEnBlanco_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(0.5, 0.5, RiskSeverity.LOW, "M", "  ", RiskStatus.IDENTIFIED));
    }

    @Test
    void risk_conStatusNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.Risk.of(0.5, 0.5, RiskSeverity.LOW, "M", "O", null));
    }

    @Test
    void risk_equals_deberiaCompararPorValor() {
        var a = RiskMatrix.Risk.of(0.5, 0.5, RiskSeverity.HIGH, "M", "O", RiskStatus.IDENTIFIED);
        var b = RiskMatrix.Risk.of(0.5, 0.5, RiskSeverity.HIGH, "M", "O", RiskStatus.IDENTIFIED);
        var c = RiskMatrix.Risk.of(0.6, 0.5, RiskSeverity.HIGH, "M", "O", RiskStatus.IDENTIFIED);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "risk");
    }

    // ------------------------------------------------------------------
    // RiskMatrix
    // ------------------------------------------------------------------

    @Test
    void empty_deberiaCrearMatrizSinRiesgos() {
        var matrix = RiskMatrix.empty();
        assertTrue(matrix.risks().isEmpty());
    }

    @Test
    void of_deberiaGuardarLaListaDeRiesgos() {
        var risk = RiskMatrix.Risk.of(0.3, 0.4, RiskSeverity.MEDIUM, "M", "O", RiskStatus.IDENTIFIED);
        var matrix = RiskMatrix.of(List.of(risk));

        assertEquals(List.of(risk), matrix.risks());
    }

    @Test
    void of_conListaNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> RiskMatrix.of(null));
    }

    @Test
    void of_conRiesgoNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> RiskMatrix.of(java.util.Arrays.asList((RiskMatrix.Risk) null)));
    }

    @Test
    void risks_deberianSerInmutables() {
        var matrix = RiskMatrix.of(List.of(
            RiskMatrix.Risk.of(0.3, 0.4, RiskSeverity.MEDIUM, "M", "O", RiskStatus.IDENTIFIED)));
        assertThrows(UnsupportedOperationException.class, () -> matrix.risks().clear());
    }

    @Test
    void equals_deberiaCompararPorValor() {
        var risk = RiskMatrix.Risk.of(0.3, 0.4, RiskSeverity.MEDIUM, "M", "O", RiskStatus.IDENTIFIED);
        assertEquals(RiskMatrix.of(List.of(risk)), RiskMatrix.of(List.of(risk)));
        assertNotEquals(RiskMatrix.of(List.of(risk)), RiskMatrix.empty());
        assertNotEquals(RiskMatrix.of(List.of(risk)), "matrix");
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        assertNotNull(RiskMatrix.empty().toString());
        assertTrue(RiskMatrix.empty().toString().contains("risks"));
    }

    // ------------------------------------------------------------------
    // Enums
    // ------------------------------------------------------------------

    @Test
    void riskSeverity_deberiaContenerLosValores() {
        assertEquals(4, RiskSeverity.values().length);
        assertEquals(RiskSeverity.CRITICAL, RiskSeverity.valueOf("CRITICAL"));
    }

    @Test
    void riskStatus_deberiaContenerLosValores() {
        assertEquals(4, RiskStatus.values().length);
        assertEquals(RiskStatus.ACCEPTED, RiskStatus.valueOf("ACCEPTED"));
    }
}
