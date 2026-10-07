package com.kinplatform.kin.medical.billing;

import com.kinplatform.kin.medical.billing.contract.ContractController;
import com.kinplatform.kin.medical.billing.contract.ContractService;
import com.kinplatform.common.config.SecurityConfig;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresion: server.servlet.context-path=/api/v1 y los controllers de billing
 * NO deben incluir /api/v1 (evita el path duplicado /api/v1/api/v1/billing/...).
 *
 * El mapping externo real es: context-path (/api/v1) + mapping (/billing/...).
 * Este slice web valida que el mapping del controller NO lleva /api/v1 y que el
 * path duplicado no resuelve (404). La validez del path externo completo se
 * confirma con el smoke test en produccion.
 */
@WebMvcTest(controllers = ContractController.class)
@Import(SecurityConfig.class)
class BillingEndpointPathTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContractService contractService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @BeforeEach
    void setUp() throws Exception {
        when(contractService.findAll(any(Pageable.class))).thenReturn(Page.empty());
        passThrough(jwtAuthenticationFilter);
        passThrough(rateLimitingFilter);
        passThrough(subscriptionAccessFilter);
    }

    private void passThrough(jakarta.servlet.Filter filter) throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void billingMappingHasNoApiV1Prefix_returns200() throws Exception {
        mockMvc.perform(get("/billing/contracts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void billingDoubledPath_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/billing/contracts"))
                .andExpect(status().isNotFound());
    }
}


