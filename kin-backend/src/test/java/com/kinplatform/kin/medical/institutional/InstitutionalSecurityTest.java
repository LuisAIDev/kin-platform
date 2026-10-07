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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BranchController.class, OrganizationMemberController.class})
@Import(SecurityConfig.class)
class InstitutionalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BranchService branchService;

    @MockBean
    private OrganizationMemberService memberService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @BeforeEach
    void setUp() throws Exception {
        when(branchService.findAll()).thenReturn(List.of());
        when(memberService.findAll()).thenReturn(List.of());
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
    @WithMockUser(roles = "IPS_ADMIN")
    void branches_asIpsAdmin_returns200() throws Exception {
        mockMvc.perform(get("/institutional/branches")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "IPS_MEDICO")
    void branches_asIpsMedico_returns200() throws Exception {
        mockMvc.perform(get("/institutional/branches")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FREE")
    void branches_asFree_returns403() throws Exception {
        mockMvc.perform(get("/institutional/branches")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void members_asPatient_returns403() throws Exception {
        mockMvc.perform(get("/institutional/members")).andExpect(status().isForbidden());
    }
}

