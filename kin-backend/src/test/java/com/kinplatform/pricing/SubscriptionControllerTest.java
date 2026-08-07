package com.kinplatform.pricing;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.pricing.dto.SubscriptionResponse;
import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    private static final String EMAIL = "u@kin.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    private MockMvc mockMvc;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionValidatorService validatorService;

    @Mock
    private PricingPlanService pricingPlanService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SubscriptionController(
                        subscriptionService, validatorService, pricingPlanService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void stubUser() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(USER_ID)
                        .email(EMAIL)
                        .role(UserRole.FREE)
                        .build()));
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());
    }

    private PricingPlan plan() {
        return PricingPlan.builder()
                .id(PLAN_ID)
                .name("Premium Pro")
                .description("Desc")
                .price(new BigDecimal("29.00"))
                .features("[]")
                .messagesPerMonth(100)
                .maxProjects(10)
                .supportLevel(SupportLevel.PREMIUM)
                .viabilityScoringDetail(ViabilityScoringDetail.DETAILED)
                .build();
    }

    private SubscriptionResponse subscriptionResponse() {
        return SubscriptionResponse.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(0)
                .messagesPerMonth(100)
                .build();
    }

    @Test
    void getCurrentSubscription_deberiaResponder200() throws Exception {
        stubUser();
        when(subscriptionService.getCurrentSubscription(USER_ID)).thenReturn(subscriptionResponse());

        mockMvc.perform(get("/subscriptions/current").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void subscribe_deberiaResponder201() throws Exception {
        stubUser();
        when(subscriptionService.subscribe(USER_ID, PLAN_ID)).thenReturn(subscriptionResponse());

        mockMvc.perform(post("/subscriptions")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + PLAN_ID + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelSubscription_deberiaResponder200() throws Exception {
        stubUser();
        when(subscriptionService.cancelSubscription(USER_ID)).thenReturn(subscriptionResponse());

        mockMvc.perform(post("/subscriptions/cancel").principal(principal())).andExpect(status().isOk());
    }

    @Test
    void getStatus_deberiaResponder200() throws Exception {
        stubUser();
        when(validatorService.isSubscriptionActive(USER_ID)).thenReturn(true);
        when(validatorService.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(validatorService.getRemainingMessages(USER_ID)).thenReturn(100);
        when(validatorService.canCreateProject(USER_ID)).thenReturn(true);
        when(validatorService.getAvailableAILevel(USER_ID)).thenReturn("PRO");

        mockMvc.perform(get("/subscriptions/status").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.planName").value("Premium Pro"))
                .andExpect(jsonPath("$.remainingMessages").value(100))
                .andExpect(jsonPath("$.canCreateProject").value(true))
                .andExpect(jsonPath("$.aiLevel").value("PRO"))
                .andExpect(jsonPath("$.messagesPerMonth").value(100))
                .andExpect(jsonPath("$.maxProjects").value(10))
                .andExpect(jsonPath("$.advancedAI").value(false))
                .andExpect(jsonPath("$.supportLevel").value("PREMIUM"));
    }

    @Test
    void getAvailableUpgrades_deberiaResponder200() throws Exception {
        stubUser();
        when(validatorService.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(pricingPlanService.getActivePlans())
                .thenReturn(List.of(
                        plan(),
                        PricingPlan.builder()
                                .id(UUID.randomUUID())
                                .name("Empresarial")
                                .price(new BigDecimal("99.00"))
                                .features("[]")
                                .build()));

        mockMvc.perform(get("/subscriptions/available-upgrades").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Empresarial"));
    }

    @Test
    void startTrial_deberiaResponder201() throws Exception {
        stubUser();
        when(pricingPlanService.getPlanByName("Premium Pro")).thenReturn(Optional.of(plan()));
        when(subscriptionService.startTrial(USER_ID, PLAN_ID)).thenReturn(subscriptionResponse());

        mockMvc.perform(post("/subscriptions/trial").principal(principal())).andExpect(status().isCreated());
    }

    @Test
    void startTrial_planNoEncontrado_deberiaFallar() throws Exception {
        stubUser();
        when(pricingPlanService.getPlanByName("Premium Pro")).thenReturn(Optional.empty());

        mockMvc.perform(post("/subscriptions/trial").principal(principal())).andExpect(status().isNotFound());
    }
}
