package com.kinplatform.common.config;

import com.kinplatform.billing.fev.FevRipsController;
import com.kinplatform.billing.fev.FevRipsInvoice;
import com.kinplatform.billing.fev.FevRipsService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FevRipsController.class)
@Import(SecurityConfig.class)
class SecurityConfigBillingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FevRipsService fevRipsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @BeforeEach
    void passThroughFilters() throws Exception {
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
    void billing_admin_canAccessFevRips() throws Exception {
        UUID id = UUID.randomUUID();
        when(fevRipsService.getStatus(id)).thenReturn(FevRipsInvoice.builder()
                .id(id).status(FevRipsInvoice.InvoiceStatus.ACCEPTED).build());

        mockMvc.perform(get("/api/v1/billing/fev-rips/{id}/status", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void billing_patient_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/billing/fev-rips/{id}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FREE")
    void billing_userWithoutBillingRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/billing/fev-rips/{id}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void billing_anonymous_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/billing/fev-rips/{id}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
