package com.kinplatform.pricing.stripe;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/stripe/patient")
@RequiredArgsConstructor
public class PatientStripeController {

    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final PricingPlanRepository planRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutResponse> createCheckoutSessionPatient(
            Authentication auth,
            @Valid @RequestBody CheckoutRequest request) {

        User user = AuthenticatedUsers.require(userRepository, auth);

        if (user.getRole() != UserRole.PATIENT) {
            throw new AccessDeniedException("Solo pacientes pueden acceder a este endpoint");
        }

        var plan = planRepository
                .findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + request.getPlanId()));

        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("El plan no está activo");
        }

        if (plan.getVertical() != ProductVertical.SALUD_PERSONAL) {
            throw new IllegalArgumentException("El plan debe ser de la vertical SALUD_PERSONAL");
        }

        if (plan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Los planes gratuitos no requieren pago");
        }

        // Verificar que el paciente no tenga ya una suscripción activa al mismo plan
        // (opcional, según reglas de negocio)

        var defaultSuccess = "https://kin-platform.com/dashboard/patient/plans?success=true";
        var defaultCancel = "https://kin-platform.com/dashboard/patient/plans?canceled=true";

        var response = stripeService.createCheckoutSession(
                user.getId(),
                request.getPlanId(),
                request.getSuccessUrl() != null ? request.getSuccessUrl() : defaultSuccess,
                request.getCancelUrl() != null ? request.getCancelUrl() : defaultCancel);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}