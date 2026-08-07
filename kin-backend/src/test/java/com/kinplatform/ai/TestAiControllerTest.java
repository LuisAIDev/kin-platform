package com.kinplatform.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class TestAiControllerTest {

    @Mock
    private ChatClient deepseekClient;

    @Mock
    private ChatClient.ChatClientRequestSpec spec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private TestAiController controller;

    @BeforeEach
    void setUp() {
        controller = new TestAiController(deepseekClient, "deepseek-v4-flash");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDeepSeek_deberiaDevolverExito() {
        when(deepseekClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("OK funciono");

        Map<String, Object> result = controller.testDeepSeek().getBody();

        assertEquals("success", result.get("status"));
        assertEquals("deepseek-v4-flash", result.get("model"));
        assertEquals("OK funciono", result.get("response"));
        assertEquals(11, result.get("response_length"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDeepSeek_conError_deberiaDevolverError() {
        when(deepseekClient.prompt()).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenThrow(new RuntimeException("llm down"));

        Map<String, Object> result = controller.testDeepSeek().getBody();

        assertEquals("error", result.get("status"));
        assertTrue(((String) result.get("exception_message")).contains("llm down"));
    }
}
