package com.kinplatform.kin.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConversationPhaseTest {

    @Test
    void phases_deberiaContenerLasTresFases() {
        var values = ConversationPhase.values();
        assertEquals(3, values.length);
        assertSame(ConversationPhase.EXPLORATION, ConversationPhase.valueOf("EXPLORATION"));
        assertSame(ConversationPhase.REPORTING, ConversationPhase.valueOf("REPORTING"));
        assertSame(ConversationPhase.CLOSED, ConversationPhase.valueOf("CLOSED"));
    }
}
