package com.kinplatform.kin.conversation;

import com.kinplatform.kin.context.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTurnTest {

    @Test
    void turn_deberiaExponerCampos() {
        var projectId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var history = List.of(Message.user("hola"), Message.assistant("¿qué proyecto?"));
        var turn = new ConversationTurn(projectId, userId, "es de ventas", history,
            "Mi proyecto", "Descripción", "TECHNOLOGY");

        assertEquals(projectId, turn.projectId());
        assertEquals(userId, turn.userId());
        assertEquals("es de ventas", turn.userMessage());
        assertEquals(history, turn.history());
        assertEquals("Mi proyecto", turn.projectTitle());
        assertEquals("Descripción", turn.projectDescription());
        assertEquals("TECHNOLOGY", turn.projectCategory());
    }

    @Test
    void turn_deberiaProtegerElHistorial() {
        var history = new ArrayList<>(List.of(Message.user("hola")));
        var turn = new ConversationTurn(UUID.randomUUID(), UUID.randomUUID(), "msg",
            history, null, null, null);

        history.add(Message.assistant("extra"));
        assertThrows(UnsupportedOperationException.class,
            () -> turn.history().add(Message.user("otro")));
        assertEquals(1, turn.history().size());
    }

    @Test
    void turn_deberiaAceptarHistorialNulo() {
        var turn = new ConversationTurn(UUID.randomUUID(), UUID.randomUUID(), "msg",
            null, null, null, null);
        assertTrue(turn.history().isEmpty());
    }

    @Test
    void turn_deberiaConservarReferenciasSimples() {
        var turn = new ConversationTurn(UUID.randomUUID(), UUID.randomUUID(), "msg",
            List.of(), null, null, null);
        assertSame(null, turn.projectTitle());
        assertSame(null, turn.projectDescription());
        assertSame(null, turn.projectCategory());
    }
}
