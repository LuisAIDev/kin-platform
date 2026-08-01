package com.kinplatform.kin.conversation.validation;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseGuardTest {

    private final ResponseGuard guard = new ResponseGuard();

    private TurnDirective directiva(ConversationPhase phase, TurnConstraints constraints,
                                    CommunicationMode mode) {
        return new TurnDirective(phase, ConversationDecision.Action.ASK,
            AnalyzedDimension.TARGET_CUSTOMER, mode, constraints);
    }

    private TurnDirective directivaQuestion() {
        return directiva(ConversationPhase.EXPLORATION,
            TurnConstraints.question(), CommunicationMode.QUESTION);
    }

    private TurnDirective directivaReport() {
        return directiva(ConversationPhase.REPORTING,
            TurnConstraints.reportExplanation(), CommunicationMode.EXPLAIN_REPORT);
    }

    @Test
    void respuestaValida_deberiaSerAceptada() {
        ResponseValidation validation = guard.validate("¿Quiénes son tus clientes?", directivaQuestion());

        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void respuestaValidaEnReporte_deberiaSerAceptada() {
        ResponseValidation validation = guard.validate(
            "El proyecto presenta una viabilidad media; fortalezas en mercado y riesgos en finanzas.",
            directivaReport());

        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void respuestaVacia_deberiaRechazarConEmpty() {
        ResponseValidation validation = guard.validate("", directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.empty"));
    }

    @Test
    void respuestaBlank_deberiaRechazarConEmpty() {
        ResponseValidation validation = guard.validate("   ", directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.empty"));
    }

    @Test
    void respuestaDemasiadoLarga_deberiaRechazarConTooLong() {
        String larga = "a".repeat(TurnConstraints.QUESTION_MAX_LENGTH + 1);

        ResponseValidation validation = guard.validate(larga, directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.too_long"));
    }

    @Test
    void respuestaEnLimiteExacto_deberiaSerAceptada() {
        String exacta = "a".repeat(TurnConstraints.QUESTION_MAX_LENGTH);

        ResponseValidation validation = guard.validate(exacta, directivaQuestion());

        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void respuestaEnLimiteMasUno_deberiaRechazar() {
        String excedida = "a".repeat(TurnConstraints.REPORT_EXPLANATION_MAX_LENGTH + 1);

        ResponseValidation validation = guard.validate(excedida, directivaReport());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.too_long"));
    }

    @Test
    void respuestaConMultiplesPreguntas_deberiaRechazar() {
        ResponseValidation validation = guard.validate("¿Qué problema resuelves? ¿Tienes clientes?",
            directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.multiple_questions"));
    }

    @Test
    void respuestaConUnaSolaPregunta_deberiaSerAceptada() {
        ResponseValidation validation = guard.validate("¿Cuál es tu modelo de ingresos?", directivaQuestion());

        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void reporteSinPreguntaUnica_noDeberiaRechazarMultiplesPreguntas() {
        ResponseValidation validation = guard.validate(
            "El scoring fue 62? La viabilidad es media? Los riesgos bajos.", directivaReport());

        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void marcadorProhibido_enExploracion_deberiaRechazar() {
        ResponseValidation validation = guard.validate(
            "Resumen === CONSULTING REPORT === oculto", directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.forbidden_marker"));
    }

    @Test
    void marcadorInforme_deberiaRechazarEnExploracion() {
        ResponseValidation validation = guard.validate(
            "## INFORME DE VIABILIDAD\nresumen", directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.forbidden_marker"));
    }

    @Test
    void marcadorScoring_deberiaRechazarEnExploracion() {
        ResponseValidation validation = guard.validate(
            "Scoring: 62", directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.forbidden_marker"));
    }

    @Test
    void marcadorProhibido_enFaseReporting_deberiaSerAceptado() {
        ResponseValidation validation = guard.validate(
            "=== CONSULTING REPORT ===\nExplicación del reporte.", directivaReport());

        assertTrue(validation.accepted());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void multiplesIssues_deberiaAcumularse() {
        String larga = "¿a?".repeat(100) + "=== CONSULTING REPORT ===";

        ResponseValidation validation = guard.validate(larga, directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.too_long"));
        assertTrue(validation.issues().contains("response.multiple_questions"));
        assertTrue(validation.issues().contains("response.forbidden_marker"));
        assertEquals(3, validation.issues().size());
    }

    @Test
    void respuestaNull_deberiaTratarseComoVacia() {
        ResponseValidation validation = guard.validate(null, directivaQuestion());

        assertFalse(validation.accepted());
        assertTrue(validation.issues().contains("response.empty"));
    }

    @Test
    void directivaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> guard.validate("respuesta", null));
    }

    @Test
    void determinismo_deberiaProducirMismaValidacion() {
        String respuesta = "¿Qué problema resuelves?";
        var primera = guard.validate(respuesta, directivaQuestion());
        var segunda = guard.validate(respuesta, directivaQuestion());

        assertEquals(primera, segunda);
        assertEquals(primera.accepted(), segunda.accepted());
    }

    @Test
    void directivaConConstraintsPersonalizadas_deberiaRespetarlas() {
        var constraints = new TurnConstraints(50, false, List.of("SECRETO"));
        var directive = directiva(ConversationPhase.EXPLORATION, constraints, CommunicationMode.SUMMARY);

        ResponseValidation ok = guard.validate("a".repeat(50), directive);
        assertTrue(ok.accepted());

        ResponseValidation larga = guard.validate("a".repeat(51), directive);
        assertFalse(larga.accepted());
        assertTrue(larga.issues().contains("response.too_long"));

        ResponseValidation marcador = guard.validate("contiene SECRETO", directive);
        assertFalse(marcador.accepted());
        assertTrue(marcador.issues().contains("response.forbidden_marker"));
    }

    @Test
    void issueCodes_deberianSerLosDocumentados() {
        ResponseValidation vacia = guard.validate("", directivaQuestion());
        assertTrue(vacia.issues().contains("response.empty"));

        ResponseValidation larga = guard.validate("a".repeat(281), directivaQuestion());
        assertTrue(larga.issues().contains("response.too_long"));

        ResponseValidation multi = guard.validate("¿a? ¿b?", directivaQuestion());
        assertTrue(multi.issues().contains("response.multiple_questions"));

        ResponseValidation marcador = guard.validate("Scoring: 1", directivaQuestion());
        assertTrue(marcador.issues().contains("response.forbidden_marker"));
    }

    @Test
    void validacionAceptada_deberiaDevolverOk() {
        ResponseValidation validation = guard.validate("¿Tienes MVP?", directivaQuestion());

        assertTrue(validation.accepted());
        assertEquals(List.of(), validation.issues());
    }
}
