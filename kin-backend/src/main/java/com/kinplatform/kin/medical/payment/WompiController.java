package com.kinplatform.kin.medical.payment;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkout con Wompi (PSE, Nequi, tarjetas, Botón Bancolombia) para las
 * verticales de salud. Independiente de {@code StripeController}: NO comparte
 * el prefijo {@code /stripe}, de modo que Stripe sigue intacto para pagos
 * internacionales.
 */
@Slf4j
@RestController
@RequestMapping("/wompi")
@RequiredArgsConstructor
public class WompiController {

    private final WompiService wompiService;
    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckout(
            Authentication auth,
            @Valid @RequestBody CreateCheckoutRequestDto request) {

        User user = AuthenticatedUsers.require(userRepository, auth);

        PricingPlan plan = planRepository
                .findById(request.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + request.planId()));

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new IllegalArgumentException("El plan no está activo");
        }

        ProductVertical vertical = plan.getVertical();
        if (vertical != ProductVertical.SALUD_PROFESIONAL && vertical != ProductVertical.SALUD_PERSONAL) {
            throw new IllegalArgumentException("Plan no válido para médicos/pacientes");
        }

        BigDecimal priceCop = plan.getPriceCop();
        if (priceCop == null || priceCop.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El plan no tiene precio en COP configurado (price_cop)");
        }

        var session = wompiService.createCheckoutSession(new PaymentGateway.CreateCheckoutRequest(
                user.getId(),
                plan.getId(),
                plan.getCode(),
                priceCop,
                "COP",
                request.successUrl(),
                request.cancelUrl(),
                vertical.name()));

        user.setPreferredPaymentGateway("WOMPI");
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CheckoutSessionResponse(session.sessionId(), session.url()));
    }

    public record CreateCheckoutRequestDto(
            @NotNull UUID planId,
            String successUrl,
            String cancelUrl
    ) {}

    public record CheckoutSessionResponse(
            String sessionId,
            String url
    ) {}
}


