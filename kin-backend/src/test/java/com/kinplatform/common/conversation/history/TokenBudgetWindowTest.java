package com.kinplatform.common.conversation.history;

import com.kinplatform.common.context.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBudgetWindowTest {

    /** Estimador determinista: 1 carácter = 1 token. */
    private static final TokenBudgetWindow.TokenEstimator ONE_CHAR_ONE_TOKEN = String::length;

    private static List<Message> historial(int size) {
        List<Message> history = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            history.add(i % 2 == 0 ? Message.user("m" + i) : Message.assistant("m" + i));
        }
        return history;
    }

    @Test
    void limiteExacto_deberiaIncluirSoloLosMensajesQueCabenJustoEnElPresupuesto() {
        // maxInput=10, system=2 ("SS"), user=2 ("UU") -> presupuesto = 6 tokens.
        var window = new TokenBudgetWindow(ONE_CHAR_ONE_TOKEN, 10, 0);
        var history = historial(4); // 4 mensajes x 2 caracteres = 8 tokens

        var resultado = window.apply(history, "SS", "UU");

        // Entran exactamente los 3 últimos mensajes (6 tokens). El cuarto
        // (m0) haría 8 > 6 y por eso queda fuera.
        assertEquals(3, resultado.size());
        assertEquals(List.of(history.get(1), history.get(2), history.get(3)), resultado);

        int tokensIncluidos = resultado.stream()
                .mapToInt(m -> ONE_CHAR_ONE_TOKEN.estimate(m.content()))
                .sum();
        assertEquals(6, tokensIncluidos);
        assertTrue(tokensIncluidos
                + ONE_CHAR_ONE_TOKEN.estimate(history.get(0).content()) > 6,
                "agregar el mensaje anterior debe exceder el presupuesto exacto");
    }

    @Test
    void presupuestoNoPositivo_deberiaDevolverListaVaciaSinExcepcion() {
        var history = historial(4);

        // Caso negativo: el system prompt (10) excede maxInput (5).
        var negativo = new TokenBudgetWindow(ONE_CHAR_ONE_TOKEN, 5, 0);
        assertTrue(negativo.apply(history, "0123456789", "UU").isEmpty());

        // Caso cero: 2 (system) + 2 (user) = 4 = maxInput -> presupuesto 0.
        var cero = new TokenBudgetWindow(ONE_CHAR_ONE_TOKEN, 4, 0);
        assertTrue(cero.apply(history, "SS", "UU").isEmpty());
    }

    @Test
    void ordenCronologico_deberiaPreservarseEnLosMensajesRecortados() {
        // Presupuesto 6 -> entran los 3 mensajes más recientes (2 tokens c/u).
        var window = new TokenBudgetWindow(ONE_CHAR_ONE_TOKEN, 6, 0);
        var history = historial(5);

        var resultado = window.apply(history, "", "");

        assertEquals(history.subList(2, 5), resultado);
        assertEquals(List.of("m2", "m3", "m4"),
                resultado.stream().map(Message::content).toList());
        assertEquals("USER", resultado.get(0).role());
        assertEquals("ASSISTANT", resultado.get(1).role());
        assertEquals("USER", resultado.get(2).role());
    }
}
