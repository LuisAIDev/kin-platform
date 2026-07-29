package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.SubscriptionRequest;
import com.kinplatform.pricing.dto.SubscriptionResponse;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(
            Authentication auth,
            @Valid @RequestBody SubscriptionRequest request
    ) {
        var user = getCurrentUser(auth);
        var response = subscriptionService.subscribe(user.getId(), request.getPlanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> getCurrent(Authentication auth) {
        var user = getCurrentUser(auth);
        var response = subscriptionService.getCurrentSubscription(user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(Authentication auth) {
        var user = getCurrentUser(auth);
        var response = subscriptionService.cancelSubscription(user.getId());
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
