package com.kinplatform.pricing.stripe;

import com.stripe.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private final StripeService stripeService;

    public StripeWebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {
        try {
            var payload = readBody(request);
            var sigHeader = request.getHeader("Stripe-Signature");

            Event event = stripeService.constructWebhookEvent(payload, sigHeader);
            stripeService.processWebhookEvent(event);

            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.badRequest().body("Webhook error");
        }
    }

    private String readBody(HttpServletRequest request) {
        try (var reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read webhook body", e);
        }
    }
}
