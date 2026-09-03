package com.kinplatform.pricing.stripe;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;

@RestController
@RequestMapping("/stripe/patient")
@RequiredArgsConstructor
public class PatientStripeController {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutResponse> createCheckoutSessionPatient(
            Authentication auth,
            @RequestBody CheckoutRequest request) {

        User user = AuthenticatedUsers.require(userRepository, auth);

        if (user.getRole() != UserRole.PATIENT) {
            throw new AccessDeniedException("Solo pacientes pueden acceder a este endpoint");
        }

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