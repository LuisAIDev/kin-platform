package com.kinplatform.pricing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.pricing.dto.PricingPlanResponse;
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
class PricingPlanControllerTest {

    private static final String EMAIL = "admin@kin.com";
    private static final UUID PLAN_ID = UUID.randomUUID();

    private MockMvc mockMvc;

    @Mock
    private PricingPlanService pricingPlanService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PricingPlanController(pricingPlanService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void stubUser(UserRole role) {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(UUID.randomUUID())
                        .email(EMAIL)
                        .role(role)
                        .build()));
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());
    }

    private PricingPlanResponse response() {
        return PricingPlanResponse.builder()
                .id(PLAN_ID)
                .name("Plan")
                .price(BigDecimal.ZERO)
                .features(List.of("a"))
                .isActive(true)
                .build();
    }

    @Test
    void getAll_deberiaResponder200() throws Exception {
        when(pricingPlanService.getAllActive()).thenReturn(List.of(response()));

        mockMvc.perform(get("/pricing-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Plan"));
    }

    @Test
    void getById_deberiaResponder200() throws Exception {
        when(pricingPlanService.getById(PLAN_ID)).thenReturn(response());

        mockMvc.perform(get("/pricing-plans/" + PLAN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PLAN_ID.toString()));
    }

    @Test
    void create_comoAdmin_deberiaResponder201() throws Exception {
        stubUser(UserRole.ADMIN);
        when(pricingPlanService.create(any())).thenReturn(response());

        mockMvc.perform(post("/admin/pricing-plans")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plan\",\"price\":0,\"features\":[\"a\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plan"));
    }

    @Test
    void create_comoNoAdmin_deberiaResponder403() throws Exception {
        stubUser(UserRole.FREE);

        mockMvc.perform(post("/admin/pricing-plans")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plan\",\"price\":0,\"features\":[\"a\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_validacionFallida_deberiaResponder400() throws Exception {
        mockMvc.perform(post("/admin/pricing-plans")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_comoAdmin_deberiaResponder200() throws Exception {
        stubUser(UserRole.ADMIN);
        when(pricingPlanService.update(eq(PLAN_ID), any())).thenReturn(response());

        mockMvc.perform(put("/admin/pricing-plans/" + PLAN_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plan\",\"price\":0,\"features\":[\"a\"],\"isActive\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_comoNoAdmin_deberiaResponder403() throws Exception {
        stubUser(UserRole.FREE);

        mockMvc.perform(put("/admin/pricing-plans/" + PLAN_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plan\",\"price\":0,\"features\":[\"a\"],\"isActive\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_comoAdmin_deberiaResponder204() throws Exception {
        stubUser(UserRole.ADMIN);

        mockMvc.perform(delete("/admin/pricing-plans/" + PLAN_ID).principal(principal()))
                .andExpect(status().isNoContent());

        verify(pricingPlanService).deactivate(PLAN_ID);
    }

    @Test
    void deactivate_comoNoAdmin_deberiaResponder403() throws Exception {
        stubUser(UserRole.FREE);

        mockMvc.perform(delete("/admin/pricing-plans/" + PLAN_ID).principal(principal()))
                .andExpect(status().isForbidden());
    }
}
