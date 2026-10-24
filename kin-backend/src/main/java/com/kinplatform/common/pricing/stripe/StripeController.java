package com.kinplatform.common.pricing.stripe;

import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final PricingPlanRepository planRepository;

    public StripeController(
            StripeService stripeService, UserRepository userRepository, PricingPlanRepository planRepository) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    @PostMapping(value = "/create-checkout-session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CheckoutResponse> createCheckoutSession(
            Authentication auth, @Valid @RequestBody CheckoutRequest request) {
        var user = getCurrentUser(auth);

        var plan = planRepository
                .findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + request.getPlanId()));

        if (plan.getVertical() != ProductVertical.EMPRESAS) {
            throw new IllegalArgumentException("Pricing plan does not belong to EMPRESAS vertical");
        }

        var defaultSuccess = "http://localhost:3000/dashboard/plans?success=true";
        var defaultCancel = "http://localhost:3000/dashboard/plans?cancelled=true";

        var response = stripeService.createCheckoutSession(
                user.getId(),
                request.getPlanId(),
                request.getSuccessUrl() != null ? request.getSuccessUrl() : defaultSuccess,
                request.getCancelUrl() != null ? request.getCancelUrl() : defaultCancel);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private User getCurrentUser(Authentication auth) {
        return com.kinplatform.common.security.AuthenticatedUsers.require(userRepository, auth);
    }
}


