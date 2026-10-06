package com.kinplatform.platform.chat.dto;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * El campo {@code action} es aditivo: por defecto es {@code null} y se
 * serializa sin romper el contrato de las respuestas existentes del chat.
 */
class ChatResponseActionCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void actionEsNullPorDefecto() {
        ChatResponse response =
                ChatResponse.builder().content("respuesta").tokensUsed(0).build();
        assertNull(response.getAction());
    }

    @Test
    void serializaActionComoNullSinRomperElContrato() throws Exception {
        ChatResponse response = ChatResponse.builder().content("respuesta").build();
        String json = objectMapper.writeValueAsString(response);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"action\":null"));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"content\":\"respuesta\""));
    }
}

