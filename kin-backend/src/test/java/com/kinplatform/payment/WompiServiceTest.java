package com.kinplatform.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.eventbus.IdempotencyService;
import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests de la lógica crítica de Wompi: firma de integridad, validación del
 * checksum del webhook (seguridad) y parseo de la referencia
 * {@code KIN_<userId>_<planId>_<ts>} (que con separador "-" nunca funcionaría
 * porque los UUID contienen guiones).
 */
@ExtendWith(MockitoExtension.class)
class WompiServiceTest {

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private SubscriptionActivationService activationService;

    @Mock
    private IdempotencyService idempotencyService;

    private WompiProperties props;
    private WompiService service;

    @BeforeEach
    void setUp() {
        props = new WompiProperties();
        props.setPublicKey("pub_prod_test");
        props.setIntegritySecret("prod_integrity_test");
        props.setEventsSecret("prod_events_test");
        service = new WompiService(props, planRepository, activationService, idempotencyService, new ObjectMapper());
    }

    @Test
    void integritySignature_deberiaSerSha256DeLosCamposEnOrden() {
        String reference = "KIN_abc_def_123";
        long amount = 3700000L;

        String expected = sha256(reference + amount + "COP" + props.getIntegritySecret());

        assertEquals(expected, service.integritySignature(reference, amount, "COP"));
    }

    @Test
    void createCheckoutSession_deberiaConstruirUrlWebCheckout() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        var plan = PricingPlan.builder()
                .id(planId)
                .code("PROFESSIONAL")
                .name("Profesional")
                .price(new BigDecimal("35.00"))
                .priceCop(new BigDecimal("140000"))
                .features("[]")
                .isActive(true)
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        var session = service.createCheckoutSession(new PaymentGateway.CreateCheckoutRequest(
                userId, planId, "PROFESSIONAL", new BigDecimal("140000"), "COP",
                "https://app/success", null, "SALUD_PROFESIONAL"));

        assertTrue(session.url().startsWith("https://checkout.wompi.co/p/?"));
        assertTrue(session.url().contains("amount-in-cents=14000000"));
        assertTrue(session.url().contains("currency=COP"));
        assertTrue(session.url().contains("signature:integrity="));
        assertTrue(session.url().contains("redirect-url="));
        assertTrue(session.sessionId().startsWith("KIN_" + userId + "_" + planId + "_"));
    }

    @Test
    void createCheckoutSession_sinPriceCop_deberiaFallar() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        var plan = PricingPlan.builder()
                .id(planId)
                .code("PROFESSIONAL")
                .name("Profesional")
                .price(new BigDecimal("35.00"))
                .features("[]")
                .isActive(true)
                .build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        var request = new PaymentGateway.CreateCheckoutRequest(
                userId, planId, "PROFESSIONAL", BigDecimal.ZERO, "COP", null, null, "SALUD_PROFESIONAL");

        assertThrows(IllegalArgumentException.class, () -> service.createCheckoutSession(request));
    }

    @Test
    void webhook_aprobado_deberiaActivarPorReferencia() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        String reference = "KIN_" + userId + "_" + planId + "_" + System.currentTimeMillis();
        String payload = eventPayload("tx-ok", reference, "APPROVED");
        when(idempotencyService.isProcessed("wompi:tx-ok")).thenReturn(false);

        service.handleWebhook(payload, checksum(payload));

        verify(idempotencyService).markProcessed("wompi:tx-ok", userId, "wompi.transaction.updated");
        verify(activationService).activateOrRenew(userId, planId);
    }

    @Test
    void webhook_duplicado_noDeberiaActivar() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        String reference = "KIN_" + userId + "_" + planId + "_" + System.currentTimeMillis();
        String payload = eventPayload("tx-dup", reference, "APPROVED");
        when(idempotencyService.isProcessed("wompi:tx-dup")).thenReturn(true);

        service.handleWebhook(payload, checksum(payload));

        verify(activationService, never()).activateOrRenew(any(), any());
    }

    @Test
    void webhook_checksumInvalido_deberiaLanzarSecurityException() {
        String payload = eventPayload("tx-bad", "KIN_x_y_1", "APPROVED");

        assertThrows(SecurityException.class, () -> service.handleWebhook(payload, "DEADBEEF"));
    }

    @Test
    void webhook_sinEventsSecret_deberiaRechazar() {
        props.setEventsSecret("");
        String payload = eventPayload("tx-ns", "KIN_x_y_1", "APPROVED");

        assertThrows(SecurityException.class, () -> service.handleWebhook(payload, "DEADBEEF"));
    }

    private String eventPayload(String txId, String reference, String status) {
        return """
                {"event":"transaction.updated","data":{"transaction":{"id":"%s","status":"%s","reference":"%s","amount_in_cents":3700000}},"signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"]},"timestamp":1530291411,"sent_at":"2018-07-20T16:45:05.000Z"}
                """
                .formatted(txId, status, reference);
    }

    @SuppressWarnings("unchecked")
    private String checksum(String payload) {
        try {
            Map<String, Object> event = new ObjectMapper().readValue(payload, Map.class);
            Map<String, Object> signature = (Map<String, Object>) event.get("signature");
            List<String> properties = (List<String>) signature.get("properties");
            Map<String, Object> data = (Map<String, Object>) event.get("data");

            StringBuilder concatenated = new StringBuilder();
            for (String property : properties) {
                Object current = data;
                for (String segment : property.split("\\.")) {
                    current = ((Map<String, Object>) current).get(segment);
                }
                concatenated.append(current);
            }
            concatenated.append(((Number) event.get("timestamp")).longValue());
            concatenated.append(props.getEventsSecret());
            return sha256(concatenated.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sha256(String data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
