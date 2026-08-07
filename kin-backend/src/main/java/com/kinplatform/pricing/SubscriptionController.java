package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.PricingPlanDTO;
import com.kinplatform.pricing.dto.SubscriptionRequest;
import com.kinplatform.pricing.dto.SubscriptionResponse;
import com.kinplatform.pricing.dto.SubscriptionStatusResponse;
import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionValidatorService validatorService;
    private final PricingPlanService pricingPlanService;
    private final UserRepository userRepository;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            SubscriptionValidatorService validatorService,
            PricingPlanService pricingPlanService,
            UserRepository userRepository) {
        this.subscriptionService = subscriptionService;
        this.validatorService = validatorService;
        this.pricingPlanService = pricingPlanService;
        this.userRepository = userRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(Authentication auth) {
        User user = getCurrentUser(auth);
        log.info("Obteniendo suscripción actual para usuario: {}", user.getId());

        SubscriptionResponse response = subscriptionService.getCurrentSubscription(user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> subscribe(
            Authentication auth, @Valid @RequestBody SubscriptionRequest request) {
        User user = getCurrentUser(auth);
        log.info("Usuario {} suscribiéndose al plan: {}", user.getId(), request.getPlanId());

        SubscriptionResponse response = subscriptionService.subscribe(user.getId(), request.getPlanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(Authentication auth) {
        User user = getCurrentUser(auth);
        log.info("Usuario {} cancelando suscripción", user.getId());

        SubscriptionResponse response = subscriptionService.cancelSubscription(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(Authentication auth) {
        User user = getCurrentUser(auth);
        UUID userId = user.getId();

        boolean isActive = validatorService.isSubscriptionActive(userId);
        PricingPlan plan = validatorService.getCurrentPlan(userId);
        int remainingMessages = validatorService.getRemainingMessages(userId);
        boolean canCreateProject = validatorService.canCreateProject(userId);
        String aiLevel = validatorService.getAvailableAILevel(userId);

        SubscriptionStatusResponse response = SubscriptionStatusResponse.builder()
                .isActive(isActive)
                .planName(plan.getName())
                .planDescription(plan.getDescription())
                .remainingMessages(remainingMessages)
                .canCreateProject(canCreateProject)
                .aiLevel(aiLevel)
                .messagesPerMonth(plan.getMessagesPerMonth())
                .maxProjects(plan.getMaxProjects())
                .advancedAI(plan.getAdvancedAI())
                .pdfExport(plan.getPdfExport())
                .supportLevel(plan.getSupportLevel().name())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/available-upgrades")
    public ResponseEntity<List<PricingPlanDTO>> getAvailableUpgrades(Authentication auth) {
        User user = getCurrentUser(auth);
        UUID userId = user.getId();

        PricingPlan currentPlan = validatorService.getCurrentPlan(userId);
        List<PricingPlan> allPlans = pricingPlanService.getActivePlans();

        List<PricingPlanDTO> upgrades = allPlans.stream()
                .filter(plan -> plan.getPrice().compareTo(currentPlan.getPrice()) > 0)
                .map(PricingPlanDTO::fromEntity)
                .toList();

        log.info("Usuario {} tiene {} planes de upgrade disponibles", userId, upgrades.size());

        return ResponseEntity.ok(upgrades);
    }

    @PostMapping("/trial")
    public ResponseEntity<SubscriptionResponse> startTrial(Authentication auth) {
        User user = getCurrentUser(auth);
        UUID userId = user.getId();
        log.info("Usuario {} iniciando período de prueba", userId);

        PricingPlan premiumPlan = pricingPlanService
                .getPlanByName("Premium Pro")
                .orElseThrow(() -> new PlanNotFoundException("Plan Premium Pro no encontrado"));

        SubscriptionResponse response = subscriptionService.startTrial(userId, premiumPlan.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private User getCurrentUser(Authentication auth) {
        return com.kinplatform.common.security.AuthenticatedUsers.require(userRepository, auth);
    }
}
