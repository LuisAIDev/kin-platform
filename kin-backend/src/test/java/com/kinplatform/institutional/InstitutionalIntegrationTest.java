package com.kinplatform.institutional;

import com.kinplatform.common.config.SecurityConfig;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import com.kinplatform.user.UserRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo de gestion institucional sin BD (slice web + servicios mockeados).
 */
@WebMvcTest(controllers = {BranchController.class, OrganizationMemberController.class})
@Import(SecurityConfig.class)
class InstitutionalIntegrationTest {

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
    void createBranch_returns201() throws Exception {
        when(branchService.create(any())).thenReturn(Branch.builder()
                .id(UUID.randomUUID()).name("Sede Centro").build());

        mockMvc.perform(post("/institutional/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sede Centro\",\"city\":\"Bogota\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sede Centro"));
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void inviteMember_returns201() throws Exception {
        when(memberService.invite(any())).thenReturn(OrganizationMember.builder()
                .id(UUID.randomUUID()).role(UserRole.IPS_MEDICO)
                .status(OrganizationMember.MemberStatus.INVITED).build());

        mockMvc.perform(post("/institutional/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + UUID.randomUUID() + "\",\"role\":\"IPS_MEDICO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("IPS_MEDICO"));
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void acceptMember_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(memberService.accept(id)).thenReturn(OrganizationMember.builder()
                .id(id).status(OrganizationMember.MemberStatus.ACTIVE).build());

        mockMvc.perform(post("/institutional/members/{id}/accept", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
