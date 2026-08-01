package com.kinplatform.kin.conversation.history;

import com.kinplatform.kin.context.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryWindowTest {

    private final HistoryWindow window = new HistoryWindow();

    private List<Message> historial(int size) {
        List<Message> history = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            history.add(i % 2 == 0 ? Message.user("usuario-" + i) : Message.assistant("asistente-" + i));
        }
        return history;
    }

    @Test
    void historialVacio_deberiaDevolverListaVacia() {
        var resultado = window.window(List.of(), 20);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void unMensaje_deberiaConservarlo() {
        var history = List.of(Message.user("solo"));

        var resultado = window.window(history, 20);

        assertEquals(List.of(Message.user("solo")), resultado);
        assertSame(history.get(0), resultado.get(0));
    }

    @Test
    void historialMenorAlLimite_deberiaConservarTodo() {
        var history = historial(3);

        var resultado = window.window(history, 5);

        assertEquals(history, resultado);
        assertEquals(3, resultado.size());
    }

    @Test
    void historialIgualAlLimite_deberiaConservarTodo() {
        var history = historial(5);

        var resultado = window.window(history, 5);

        assertEquals(history, resultado);
        assertEquals(5, resultado.size());
    }

    @Test
    void historialMayorAlLimite_deberiaConservarSoloLosUltimos() {
        var history = historial(10);

        var resultado = window.window(history, 4);

        assertEquals(4, resultado.size());
        assertEquals(history.subList(6, 10), resultado);
    }

    @Test
    void ventanaConUnSoloMensaje_deberiaConservarElUltimo() {
        var history = historial(8);

        var resultado = window.window(history, 1);

        assertEquals(List.of(history.get(7)), resultado);
    }

    @Test
    void ordenCronologico_deberiaPreservarse() {
        var history = historial(12);

        var resultado = window.window(history, 6);

        assertEquals(history.subList(6, 12), resultado);
        assertSame(history.get(6), resultado.get(0));
        assertSame(history.get(11), resultado.get(5));
    }

    @Test
    void mensajeDelUsuarioActual_deberiaEstarSiempreIncluido() {
        var history = historial(9);
        Message ultimoUsuario = Message.user("turno-actual");

        var resultado = window.window(history, 3);

        assertSame(history.get(8), resultado.get(2));
        assertEquals("usuario-8", resultado.get(2).content());
        assertSame(ultimoUsuario.role(), resultado.get(2).role());
    }

    @Test
    void mensajes_deberianConservarContenidoSinAlteracion() {
        var history = historial(7);

        var resultado = window.window(history, 4);

        assertEquals(history.get(3).content(), resultado.get(0).content());
        assertEquals(history.get(4).content(), resultado.get(1).content());
        assertEquals(history.get(5).content(), resultado.get(2).content());
        assertEquals(history.get(6).content(), resultado.get(3).content());
    }

    @Test
    void coleccionDevuelta_deberiaSerInmutable() {
        var history = historial(6);

        var resultado = window.window(history, 3);

        assertThrows(UnsupportedOperationException.class, () -> resultado.add(Message.user("extra")));
    }

    @Test
    void mutacionDelHistorialOriginal_noDeberiaAfectarLaVentana() {
        var history = new ArrayList<>(historial(6));

        var resultado = window.window(history, 3);
        history.add(Message.user("nuevo"));

        assertEquals(3, resultado.size());
        assertEquals("usuario-4", resultado.get(1).content());
    }

    @Test
    void determinismo_deberiaProducirLaMismaVentana() {
        var history = historial(8);

        var primera = window.window(history, 3);
        var segunda = window.window(history, 3);

        assertEquals(primera, segunda);
        assertEquals(primera, window.window(new ArrayList<>(history), 3));
    }

    @Test
    void historialNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> window.window(null, 20));
    }

    @Test
    void limiteCero_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> window.window(historial(3), 0));
    }

    @Test
    void limiteNegativo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> window.window(historial(3), -1));
    }

    @Test
    void constantes_defaultMaxMessages_deberiaSerVeinte() {
        assertEquals(20, HistoryWindow.DEFAULT_MAX_MESSAGES);
    }

    @Test
    void ventana_deberiaConservarReferenciasDeLosMensajes() {
        var history = historial(5);

        var resultado = window.window(history, 5);

        for (int i = 0; i < history.size(); i++) {
            assertSame(history.get(i), resultado.get(i));
        }
    }
}
