package com.kinplatform.pricing.stripe;

import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StripeService {

    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final StripeWebhookEventRepository webhookEventRepository;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeService(
            PricingPlanRepository planRepository,
            UserRepository userRepository,
            UserSubscriptionRepository subscriptionRepository,
            StripeWebhookEventRepository webhookEventRepository) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webhookEventRepository = webhookEventRepository;
    }

    @PostConstruct
    void warnIfMissingConfig() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("STRIPE_WEBHOOK_SECRET no está configurada — los webhooks de Stripe fallarán hasta que se defina");
        }
    }

    public CheckoutResponse createCheckoutSession(UUID userId, UUID planId, String successUrl, String cancelUrl) {
        var user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var plan = planRepository
                .findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + planId));

        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("Pricing plan is not active");
        }

        if (plan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Free plans do not require payment");
        }

        try {
            var paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(user.getEmail())
                    .setClientReferenceId(userId.toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(plan.getPrice()
                                            .multiply(java.math.BigDecimal.valueOf(100))
                                            .longValue())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(plan.getName())
                                            .setDescription(plan.getDescription())
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("plan_id", planId.toString())
                    .putMetadata("user_id", userId.toString());

            if (successUrl != null) {
                paramsBuilder.setSuccessUrl(successUrl);
            }
            if (cancelUrl != null) {
                paramsBuilder.setCancelUrl(cancelUrl);
            }

            var session = Session.create(paramsBuilder.build());

            log.info(
                    "Stripe checkout session created: {} for user {} plan {}", session.getId(), userId, plan.getName());

            return CheckoutResponse.builder()
                    .sessionId(session.getId())
                    .url(session.getUrl())
                    .build();
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session", e);
            throw new RuntimeException("Payment processing error: " + e.getMessage());
        }
    }

    @Transactional
    public void handleCheckoutCompleted(String sessionId) {
        try {
            var session = Session.retrieve(sessionId);

            var userId = UUID.fromString(session.getClientReferenceId());
            var planId = UUID.fromString(session.getMetadata().get("plan_id"));

            var user = userRepository
                    .findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            var plan = planRepository
                    .findById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

            subscriptionRepository
                    .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                    .ifPresent(s -> {
                        throw new IllegalArgumentException("User already has an active subscription");
                    });

            var now = OffsetDateTime.now();
            var endDate = now.plusMonths(1);

            var subscription = UserSubscription.builder()
                    .user(user)
                    .plan(plan)
                    .startDate(now)
                    .endDate(endDate)
                    .status(SubscriptionStatus.ACTIVE)
                    .messagesUsed(0)
                    .lastResetDate(now)
                    .build();

            var saved = subscriptionRepository.save(subscription);

            user.setCurrentPlan(plan);
            user.setSubscription(saved);
            userRepository.save(user);

            log.info(
                    "Subscription activated after checkout: user {} plan {} session {}",
                    userId,
                    plan.getName(),
                    sessionId);
        } catch (Exception e) {
            log.error("Failed to process checkout completed event for session {}", sessionId, e);
            throw new RuntimeException("Failed to activate subscription", e);
        }
    }

    /**
     * Procesa un evento de webhook de forma idempotente. El registro del evento
     * y su efecto (p. ej. activar la suscripci�n) viven en la misma transacci�n:
     * si el evento ya fue procesado, la columna UNIQUE {@code webhook_events.event_id}
     * lanza {@link DataIntegrityViolationException} al hacer flush y el m�todo
     * devuelve {@code false} sin efectos secundarios.
     *
     * @return {@code true} si el evento se proces�, {@code false} si era un duplicado.
     */
    @Transactional
    public boolean processWebhookEvent(Event event) {
        try {
            webhookEventRepository.saveAndFlush(new StripeWebhookEvent(event.getId(), event.getType()));
        } catch (DataIntegrityViolationException e) {
            log.info("Webhook event {} ({}), ya procesado - se omite", event.getId(), event.getType());
            return false;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                var session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));
                handleCheckoutCompleted(session.getId());
                log.info("Checkout session completed: {}", session.getId());
            }
            case "checkout.session.expired" -> log.warn("Checkout session expired");
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
        return true;
    }

    public Event constructWebhookEvent(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "STRIPE_WEBHOOK_SECRET no está configurada. Define la variable de entorno en Render y redeployea.");
        }
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            throw new RuntimeException("Webhook signature verification failed", e);
        }
    }
}
