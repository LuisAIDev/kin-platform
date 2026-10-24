package com.kinplatform.kin.medical.payment;

import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.SubscriptionStatus;
import com.kinplatform.common.pricing.UserSubscription;
import com.kinplatform.common.pricing.UserSubscriptionRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de activación/renovación de suscripciones, compartida por todas las
 * pasarelas (Stripe y Wompi). Antes vivía duplicada en {@code StripeService} y
 * {@code WompiService}; centralizarla garantiza que un pago aprobado por
 * cualquier pasarela produzca exactamente el mismo estado en la base de datos.
 *
 * <p>Dos semánticas, que reflejan el comportamiento histórico de Stripe:</p>
 * <ul>
 *   <li>{@link #activateNew(UUID, UUID)}: alta de suscripción tras un primer
 *       pago. Falla si el usuario ya tiene una suscripción {@code ACTIVE} en la
 *       misma vertical (evita doble alta).</li>
 *   <li>{@link #activateOrRenew(UUID, UUID)}: renueva una suscripción
 *       {@code ACTIVE} vigente de la misma vertical (extiende un mes y resetea
 *       consumo) o la crea si no existe. Es la semántica usada para pagos
 *       recurrentes y para Wompi, cuyo pago es puntual y puede repetirse mes a
 *       mes.</li>
 * </ul>
 *
 * <p>Todas las búsquedas se filtran por la vertical del plan pagado: un pago de
 * EMPRESAS nunca renueva ni bloquea una suscripción de salud, y viceversa. Así
 * un usuario puede mantener una suscripción activa por vertical.</p>
 *
 * <p>El estado {@code TRIAL} que asigna el registro de médicos no cuenta como
 * {@code ACTIVE}: un médico en trial puede contratar un plan de pago sin
 * bloqueos.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionActivationService {

    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    /**
     * Da de alta una suscripción nueva. Falla si ya existe una {@code ACTIVE}
     * en la misma vertical.
     */
    @Transactional
    public void activateNew(UUID userId, UUID planId) {
        var user = requireUser(userId);
        var plan = requirePlan(planId);

        subscriptionRepository
                .findByUserAndPlanVerticalAndStatus(userId, plan.getVertical(), SubscriptionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("User already has an active subscription");
                });

        var saved = subscriptionRepository.save(buildSubscription(user, plan));
        user.setCurrentPlan(plan);
        user.setSubscription(saved);
        userRepository.save(user);

        log.info("Suscripción activada: user={} plan={}", userId, plan.getName());
    }

    /**
     * Renueva la suscripción {@code ACTIVE} vigente de la misma vertical o crea
     * una nueva si no existe. Idéntico a la semántica de renovación de Stripe.
     */
    @Transactional
    public void activateOrRenew(UUID userId, UUID planId) {
        var user = requireUser(userId);
        var plan = requirePlan(planId);
        var now = OffsetDateTime.now();

        var existing = subscriptionRepository
                .findActiveByUserAndVertical(userId, SubscriptionStatus.ACTIVE, plan.getVertical(), now)
                .stream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setEndDate(now.plusMonths(1));
            existing.setMessagesUsed(0);
            existing.setLastResetDate(now);
            subscriptionRepository.save(existing);
            log.info("Suscripción renovada: user={} plan={}", userId, existing.getPlan().getName());
            return;
        }

        var saved = subscriptionRepository.save(buildSubscription(user, plan));
        user.setCurrentPlan(plan);
        user.setSubscription(saved);
        userRepository.save(user);

        log.info("Suscripción activada: user={} plan={}", userId, plan.getName());
    }

    private UserSubscription buildSubscription(User user, PricingPlan plan) {
        var now = OffsetDateTime.now();
        return UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(0)
                .lastResetDate(now)
                .build();
    }

    private User requireUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private PricingPlan requirePlan(UUID planId) {
        return planRepository
                .findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
    }
}


