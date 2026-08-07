package com.kinplatform.pricing.stripe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.stripe.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Event event;

    @Test
    void handleWebhook_deberiaResponderReceived() throws Exception {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"id\":\"evt_1\"}")));
        when(request.getHeader("Stripe-Signature")).thenReturn("sig");
        when(stripeService.constructWebhookEvent(anyString(), anyString())).thenReturn(event);
        when(stripeService.processWebhookEvent(event)).thenReturn(true);

        var controller = new StripeWebhookController(stripeService);
        var response = controller.handleWebhook(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Received", response.getBody());
    }

    @Test
    void handleWebhook_errorDeProcesamiento_deberiaResponder400() throws Exception {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{}")));
        when(request.getHeader("Stripe-Signature")).thenReturn("sig");
        when(stripeService.constructWebhookEvent(anyString(), anyString()))
                .thenThrow(new RuntimeException("bad signature"));

        var controller = new StripeWebhookController(stripeService);
        var response = controller.handleWebhook(request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Webhook error", response.getBody());
    }

    @Test
    void handleWebhook_errorDeLectura_deberiaResponder400() throws Exception {
        when(request.getReader()).thenThrow(new IOException("stream closed"));

        var controller = new StripeWebhookController(stripeService);
        var response = controller.handleWebhook(request);

        assertEquals(400, response.getStatusCode().value());
    }
}
