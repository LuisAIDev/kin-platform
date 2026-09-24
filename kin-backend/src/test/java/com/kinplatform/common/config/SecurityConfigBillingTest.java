package com.kinplatform.common.config;

import com.kinplatform.billing.contract.ContractController;
import com.kinplatform.billing.contract.ContractService;
import com.kinplatform.billing.contract.EpsContract;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FevRipsController.class, ContractController.class})
@Import(SecurityConfig.class)
class SecurityConfigBillingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FevRipsService fevRipsService;

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
        when(contractService.create(any())).thenReturn(EpsContract.builder().id(UUID.randomUUID()).build());
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

        mockMvc.perform(get("/billing/fev-rips/{id}/status", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void billing_patient_isForbidden() throws Exception {
        mockMvc.perform(get("/billing/fev-rips/{id}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FREE")
    void billing_userWithoutBillingRole_isForbidden() throws Exception {
        mockMvc.perform(get("/billing/fev-rips/{id}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void billing_anonymous_isForbidden() throws Exception {
        mockMvc.perform(get("/billing/fev-rips/{id}/status", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void contractsGET_asPhysician_returns200() throws Exception {
        mockMvc.perform(get("/billing/contracts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void contractsPOST_asPhysician_returns403() throws Exception {
        mockMvc.perform(post("/billing/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void contractsPOST_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/billing/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void contractsDELETE_asPhysician_returns403() throws Exception {
        mockMvc.perform(delete("/billing/contracts/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void contractsGET_asIPSAdmin_returns200() throws Exception {
        mockMvc.perform(get("/billing/contracts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "IPS_FACTURADOR")
    void contractsGET_asIPSFacturador_returns200() throws Exception {
        mockMvc.perform(get("/billing/contracts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "IPS_FACTURADOR")
    void contractsPOST_asIPSFacturador_returns403() throws Exception {
        mockMvc.perform(post("/billing/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_FACTURADOR")
    void fevRipsGET_asIPSFacturador_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(fevRipsService.getStatus(id)).thenReturn(FevRipsInvoice.builder()
                .id(id).status(FevRipsInvoice.InvoiceStatus.ACCEPTED).build());

        mockMvc.perform(get("/billing/fev-rips/{id}/status", id))
                .andExpect(status().isOk());
    }
}
