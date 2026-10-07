package com.kinplatform.kin.medical.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.eventbus.IdempotencyService;
import com.kinplatform.common.pricing.PricingPlanRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pasarela de pago local para Colombia (PSE, Nequi, tarjetas, Botón
 * Bancolombia) usando Wompi (Bancolombia).
 *
 * <p><b>Flujo de checkout:</b> Web Checkout. El backend construye la URL
 * {@code https://checkout.wompi.co/p/} con la llave pública, el monto en
 * centavos, una referencia propia y la firma de integridad SHA256. El usuario
 * paga en Wompi y vuelve al {@code redirect-url}. No se hace ninguna llamada
 * server-side al crear el checkout (solo se necesitan llave pública y secreto
 * de integridad).</p>
 *
 * <p><b>Activación:</b> el webhook {@code transaction.updated} con estado
 * {@code APPROVED} identifica al usuario y al plan desde la referencia
 * ({@code KIN_<userId>_<planId>_<timestamp>}) y delega en
 * {@link SubscriptionActivationService}, la misma lógica que usa Stripe.</p>
 *
 * <p><b>Moneda:</b> Wompi solo opera en COP. El monto sale de
 * {@code pricing_plans.price_cop} (no de {@code price}, que está en USD y es
 * para Stripe). Si un plan no tiene {@code price_cop}, el checkout falla con un
 * error claro en lugar de cobrar un valor incorrecto.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WompiService implements PaymentGateway {

    private static final String CURRENCY = "COP";
    private static final String REFERENCE_PREFIX = "KIN_";

    private final WompiProperties props;
    private final PricingPlanRepository planRepository;
    private final SubscriptionActivationService activationService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    public String getGatewayCode() {
        return "WOMPI";
    }

    @Override
    public CheckoutSession createCheckoutSession(CreateCheckoutRequest request) {
        requireConfig();

        if (!CURRENCY.equals(request.currency())) {
            throw new IllegalArgumentException("Wompi solo soporta moneda COP");
        }

        var plan = planRepository
                .findById(request.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + request.planId()));

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new IllegalArgumentException("El plan no está activo");
        }

        BigDecimal priceCop = plan.getPriceCop();
        if (priceCop == null || priceCop.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El plan " + plan.getCode() + " no tiene precio en COP configurado (price_cop)");
        }

        long amountInCents = priceCop.multiply(BigDecimal.valueOf(100)).longValueExact();
        String reference = REFERENCE_PREFIX + request.userId() + "_" + request.planId() + "_"
                + System.currentTimeMillis();
        String signature = integritySignature(reference, amountInCents, CURRENCY);

        StringBuilder url = new StringBuilder(props.getCheckoutUrl())
                .append("?public-key=").append(encode(props.getPublicKey()))
                .append("&currency=").append(CURRENCY)
                .append("&amount-in-cents=").append(amountInCents)
                .append("&reference=").append(encode(reference))
                .append("&signature:integrity=").append(encode(signature));
        if (request.successUrl() != null && !request.successUrl().isBlank()) {
            url.append("&redirect-url=").append(encode(request.successUrl()));
        }

        log.info("Wompi checkout generado: ref={} plan={} amountCop={}", reference, plan.getCode(), priceCop);
        return new CheckoutSession(reference, url.toString());
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!validateSignature(payload, signature)) {
            throw new SecurityException("Checksum de webhook Wompi inválido");
        }

        Map<String, Object> event = parseEvent(payload);
        String eventType = (String) event.get("event");
        if (!"transaction.updated".equals(eventType)) {
            log.debug("Evento Wompi no manejado: {}", eventType);
            return;
        }

        Map<String, Object> transaction = extractTransaction(event);
        if (transaction == null) {
            log.warn("Webhook Wompi transaction.updated sin objeto transaction");
            return;
        }

        String status = (String) transaction.get("status");
        String reference = (String) transaction.get("reference");
        String transactionId = (String) transaction.get("id");

        log.info("Wompi webhook: {} - status={} - ref={}", eventType, status, reference);

        if (!"APPROVED".equals(status)) {
            return;
        }

        UUID[] ids = parseReference(reference);

        // Idempotencia: Wompi reintenta el mismo evento hasta 3 veces. El
        // registro en processed_events (misma transacción) evita activar dos
        // veces por una sola transacción aprobada.
        String eventId = "wompi:" + transactionId;
        if (idempotencyService.isProcessed(eventId)) {
            log.info("Webhook Wompi duplicado, ignorado: {}", transactionId);
            return;
        }
        idempotencyService.markProcessed(eventId, ids[0], "wompi.transaction.updated");

        activationService.activateOrRenew(ids[0], ids[1]);
    }

    private void requireConfig() {
        if (isBlank(props.getPublicKey())) {
            throw new IllegalStateException("WOMPI_PUBLIC_KEY no configurada");
        }
        if (isBlank(props.getIntegritySecret())) {
            throw new IllegalStateException("WOMPI_INTEGRITY_SECRET no configurada");
        }
    }

    /**
     * Firma de integridad de Wompi: SHA256 de
     * {@code <reference><amountInCents><currency><integritySecret>}.
     */
    String integritySignature(String reference, long amountInCents, String currency) {
        return sha256Hex(reference + amountInCents + currency + props.getIntegritySecret());
    }

    /**
     * Valida el checksum del webhook según la especificación de Wompi:
     * SHA256 de la concatenación (en orden) de los valores de
     * {@code signature.properties} + {@code timestamp} + {@code eventsSecret}.
     * El checksum llega en el header {@code X-Event-Checksum} o en
     * {@code signature.checksum}.
     */
    boolean validateSignature(String payload, String headerChecksum) {
        if (isBlank(props.getEventsSecret())) {
            log.error("WOMPI_EVENTS_SECRET no configurada: no se puede validar el webhook");
            return false;
        }

        Map<String, Object> event = parseEvent(payload);
        if (event == null) {
            return false;
        }

        Map<String, Object> signatureObj = asMap(event.get("signature"));
        String checksum = headerChecksum;
        if (isBlank(checksum) && signatureObj != null) {
            checksum = (String) signatureObj.get("checksum");
        }
        if (isBlank(checksum)) {
            log.warn("Webhook Wompi sin checksum (header X-Event-Checksum ni signature.checksum)");
            return false;
        }

        List<?> properties = signatureObj != null ? asList(signatureObj.get("properties")) : null;
        Object timestamp = event.get("timestamp");
        if (properties == null || timestamp == null) {
            log.warn("Webhook Wompi sin signature.properties o timestamp: no se puede validar");
            return false;
        }

        Map<String, Object> data = asMap(event.get("data"));
        StringBuilder concatenated = new StringBuilder();
        for (Object property : properties) {
            concatenated.append(resolvePath(data, String.valueOf(property)));
        }
        concatenated.append(timestamp instanceof Number number
                ? String.valueOf(number.longValue())
                : String.valueOf(timestamp));
        concatenated.append(props.getEventsSecret());

        String expected = sha256Hex(concatenated.toString());
        boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                checksum.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            log.warn("Checksum de webhook Wompi no coincide");
        }
        return valid;
    }

    private Map<String, Object> parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            log.error("Payload de webhook Wompi inválido", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTransaction(Map<String, Object> event) {
        Map<String, Object> data = asMap(event.get("data"));
        if (data == null) {
            return null;
        }
        return asMap(data.get("transaction"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private List<?> asList(Object value) {
        return value instanceof List<?> list ? list : null;
    }

    /**
     * Resuelve un path con puntos (p. ej. {@code transaction.amount_in_cents})
     * dentro del objeto {@code data}. Devuelve cadena vacía si no existe, tal
     * como exige la concatenación de Wompi.
     */
    private String resolvePath(Map<String, Object> data, String path) {
        Object current = data;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return "";
            }
            current = map.get(segment);
        }
        return current == null ? "" : String.valueOf(current);
    }

    /**
     * Parsea la referencia {@code KIN_<userId>_<planId>_<timestamp>}. Se usa
     * guion bajo como separador porque los UUID contienen guiones.
     */
    private UUID[] parseReference(String reference) {
        if (reference == null || !reference.startsWith(REFERENCE_PREFIX)) {
            throw new IllegalArgumentException("Referencia Wompi inválida: " + reference);
        }
        String[] parts = reference.split("_");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Referencia Wompi inválida: " + reference);
        }
        try {
            return new UUID[] {UUID.fromString(parts[1]), UUID.fromString(parts[2])};
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Referencia Wompi inválida: " + reference, e);
        }
    }

    private String sha256Hex(String data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}



