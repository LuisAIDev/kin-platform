package com.kinplatform.kin.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeInputTest {

    @Test
    void of_deberiaEnvolverElRequest() {
        var request = KnowledgeRequest.of("mercado", java.util.List.of("retail"));
        var input = KnowledgeInput.of(request);

        assertEquals(request, input.request());
    }

    @Test
    void constructor_deberiaAceptarNulos() {
        var input = new KnowledgeInput(null);

        assertEquals(KnowledgeRequest.empty(), input.request());
    }

    @Test
    void deberiaImplementarEngineInput() {
        assertEquals(1, KnowledgeInput.class.getInterfaces().length);
        assertEquals("EngineInput", KnowledgeInput.class.getInterfaces()[0].getSimpleName());
    }
}
