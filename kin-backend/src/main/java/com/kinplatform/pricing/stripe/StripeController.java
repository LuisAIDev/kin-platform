package com.kinplatform.pricing.stripe;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    public StripeController(StripeService stripeService, UserRepository userRepository) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutResponse> createCheckoutSession(
            Authentication auth, @Valid @RequestBody CheckoutRequest request) {
        var user = getCurrentUser(auth);

        var defaultSuccess = "http://localhost:3000/dashboard/subscription?success=true";
        var defaultCancel = "http://localhost:3000/dashboard/pricing?cancelled=true";

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
