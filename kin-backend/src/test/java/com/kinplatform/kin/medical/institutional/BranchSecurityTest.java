package com.kinplatform.kin.medical.institutional;

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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BranchController.class)
@Import(SecurityConfig.class)
class BranchSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BranchService branchService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @BeforeEach
    void setUp() throws Exception {
        when(branchService.create(any())).thenReturn(Branch.builder()
                .id(UUID.randomUUID()).name("Sede").build());
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
    @WithMockUser(roles = "IPS_MEDICO")
    void createBranch_asIPSMedico_returns403() throws Exception {
        mockMvc.perform(post("/institutional/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sede X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void createBranch_asIPSAdmin_returns201() throws Exception {
        mockMvc.perform(post("/institutional/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sede X\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "IPS_FACTURADOR")
    void deleteBranch_asIPSFACTURADOR_returns403() throws Exception {
        mockMvc.perform(delete("/institutional/branches/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}

