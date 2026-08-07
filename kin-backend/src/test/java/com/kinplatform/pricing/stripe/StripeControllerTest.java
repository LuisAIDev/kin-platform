package com.kinplatform.pricing.stripe;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
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
class StripeControllerTest {

    private static final String EMAIL = "u@kin.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    private MockMvc mockMvc;

    @Mock
    private StripeService stripeService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StripeController(stripeService, userRepository))
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

    @Test
    void createCheckoutSession_deberiaResponder201() throws Exception {
        stubUser();
        when(stripeService.createCheckoutSession(
                        eq(USER_ID), eq(PLAN_ID), eq("https://app/success"), eq("https://app/cancel")))
                .thenReturn(CheckoutResponse.builder()
                        .sessionId("cs_1")
                        .url("https://checkout")
                        .build());

        mockMvc.perform(post("/stripe/create-checkout-session")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + PLAN_ID + "\","
                                + "\"successUrl\":\"https://app/success\","
                                + "\"cancelUrl\":\"https://app/cancel\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("cs_1"));
    }

    @Test
    void createCheckoutSession_sinUrls_deberiaUsarDefaults() throws Exception {
        stubUser();
        when(stripeService.createCheckoutSession(
                        eq(USER_ID),
                        eq(PLAN_ID),
                        eq("http://localhost:3000/dashboard/subscription?success=true"),
                        eq("http://localhost:3000/dashboard/pricing?cancelled=true")))
                .thenReturn(CheckoutResponse.builder().sessionId("cs_2").build());

        mockMvc.perform(post("/stripe/create-checkout-session")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + PLAN_ID + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createCheckoutSession_sinPlanId_deberiaResponder400() throws Exception {
        mockMvc.perform(post("/stripe/create-checkout-session")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCheckoutSession_usuarioNoEncontrado_deberiaResponder400() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(post("/stripe/create-checkout-session")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":\"" + PLAN_ID + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
