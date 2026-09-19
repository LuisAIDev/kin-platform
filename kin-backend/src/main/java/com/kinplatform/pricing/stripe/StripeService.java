package com.kinplatform.pricing.stripe;

import com.kinplatform.payment.PaymentGateway;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StripeService implements PaymentGateway {

    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final StripeWebhookEventRepository webhookEventRepository;
    private final com.kinplatform.payment.SubscriptionActivationService activationService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeService(
            PricingPlanRepository planRepository,
            UserRepository userRepository,
            UserSubscriptionRepository subscriptionRepository,
            StripeWebhookEventRepository webhookEventRepository,
            com.kinplatform.payment.SubscriptionActivationService activationService) {
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.activationService = activationService;
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
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
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
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("vertical", "SALUD_PERSONAL");

            // Añadir trial_period_days si el plan tiene trial_days > 0
            if (plan.getTrialDays() != null && plan.getTrialDays() > 0) {
                paramsBuilder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .setTrialPeriodDays(plan.getTrialDays().longValue())
                        .build());
            }

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

            activationService.activateNew(userId, planId);

            log.info("Subscription activated after checkout: user {} session {}", userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to process checkout completed event for session {}", sessionId, e);
            throw new RuntimeException("Failed to activate subscription", e);
        }
    }

    @Transactional
    public void handlePatientCheckoutCompleted(String sessionId) {
        try {
            var session = Session.retrieve(sessionId);

            var userId = UUID.fromString(session.getClientReferenceId());
            var planId = UUID.fromString(session.getMetadata().get("plan_id"));

            activationService.activateNew(userId, planId);

            log.info("Patient subscription activated after checkout: user {} session {}", userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to process patient checkout completed event for session {}", sessionId, e);
            throw new RuntimeException("Failed to activate patient subscription", e);
        }
    }

    @Transactional
    public void handleInvoicePaymentSucceeded(String sessionId) {
        try {
            var session = Session.retrieve(sessionId);

            var userId = UUID.fromString(session.getClientReferenceId());
            var planId = UUID.fromString(session.getMetadata().get("plan_id"));

            activationService.activateOrRenew(userId, planId);

            log.info("Subscription renewed after invoice payment: user {} session {}", userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to process invoice payment succeeded event for session {}", sessionId, e);
            throw new RuntimeException("Failed to renew subscription", e);
        }
    }

    @Transactional
    public void handlePatientInvoicePaymentSucceeded(String sessionId) {
        try {
            var session = Session.retrieve(sessionId);

            var userId = UUID.fromString(session.getClientReferenceId());
            var planId = UUID.fromString(session.getMetadata().get("plan_id"));

            activationService.activateOrRenew(userId, planId);

            log.info("Patient subscription renewed after invoice payment: user {} session {}", userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to process patient invoice payment succeeded event for session {}", sessionId, e);
            throw new RuntimeException("Failed to renew patient subscription", e);
        }
    }

    @Transactional
    public void handleInvoicePaymentFailed(UUID subscriptionId) {
        try {
            var subscription = subscriptionRepository
                    .findById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(subscription);

            log.info(
                    "Subscription marked as past due: user {} subscription {}",
                    subscription.getUser().getId(),
                    subscriptionId);
        } catch (Exception e) {
            log.error("Failed to process invoice payment failed event for subscription {}", subscriptionId, e);
            throw new RuntimeException("Failed to handle payment failure", e);
        }
    }

    @Transactional
    public void handleSubscriptionDeleted(UUID subscriptionId) {
        try {
            var subscription = subscriptionRepository
                    .findById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

            var user = subscription.getUser();

            // Cancelar la suscripción y volver al plan gratuito
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(subscription);

            // Establecer el plan actual del usuario como el plan gratuito
            var freePlan = planRepository.findFirstByIsActiveTrueOrderByPriceAsc()
                    .orElseThrow(() -> new RuntimeException("No free plan found"));
            user.setCurrentPlan(freePlan);
            user.setSubscription(null);
            userRepository.save(user);

            log.info(
                    "Subscription cancelled and user reverted to free plan: user {}",
                    user.getId());
        } catch (Exception e) {
            log.error("Failed to process subscription deleted event for subscription {}", subscriptionId, e);
            throw new RuntimeException("Failed to handle subscription deletion", e);
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

                // Verificar si es un evento de paciente
                var vertical = session.getMetadata().get("vertical");
                if ("SALUD_PERSONAL".equals(vertical)) {
                    handlePatientCheckoutCompleted(session.getId());
                } else {
                    handleCheckoutCompleted(session.getId());
                }
                log.info("Checkout session completed: {}", session.getId());
            }
            case "invoice.payment_succeeded" -> {
                var session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));
                var vertical = session.getMetadata().get("vertical");
                if ("SALUD_PERSONAL".equals(vertical)) {
                    handlePatientInvoicePaymentSucceeded(session.getId());
                } else {
                    handleInvoicePaymentSucceeded(session.getId());
                }
                log.info("Invoice payment succeeded: {}", session.getId());
            }
            case "invoice.payment_failed" -> {
                var subscriptionObj = event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Failed to deserialize subscription"));
                var subscription = (com.stripe.model.Subscription) subscriptionObj;
                handleInvoicePaymentFailed(UUID.fromString(subscription.getId()));
                log.info("Invoice payment failed: {}", subscription.getId());
            }
            case "customer.subscription.deleted" -> {
                var subscriptionObj = event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new RuntimeException("Failed to deserialize subscription"));
                var subscription = (com.stripe.model.Subscription) subscriptionObj;
                handleSubscriptionDeleted(UUID.fromString(subscription.getId()));
                log.info("Subscription deleted: {}", subscription.getId());
            }
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

    // PaymentGateway interface implementation
    @Override
    public String getGatewayCode() {
        return "STRIPE";
    }

    @Override
    public com.kinplatform.payment.PaymentGateway.CheckoutSession createCheckoutSession(
            com.kinplatform.payment.PaymentGateway.CreateCheckoutRequest request) {

        var response = createCheckoutSession(
                request.userId(),
                request.planId(),
                request.successUrl(),
                request.cancelUrl()
        );

        return new com.kinplatform.payment.PaymentGateway.CheckoutSession(
                response.getSessionId(),
                response.getUrl()
        );
    }

    @Override
    public void handleWebhook(String payload, String signature) {
        Event event = constructWebhookEvent(payload, signature);
        processWebhookEvent(event);
    }
}
