package com.kinplatform.institutional;

import com.kinplatform.common.config.SecurityConfig;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import com.kinplatform.user.User;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizationMemberController.class)
@Import(SecurityConfig.class)
class OrganizationMemberSecurityTest {

    private static final String VALID_BODY = "{\"email\":\"nuevo@clinica.com\",\"role\":\"IPS_MEDICO\"}";

    @Autowired
    private MockMvc mockMvc;

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
        when(memberService.invite(any())).thenReturn(OrganizationMember.builder()
                .id(UUID.randomUUID()).role(UserRole.IPS_MEDICO)
                .status(OrganizationMember.MemberStatus.INVITED).build());
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
    @WithMockUser(roles = "IPS_FACTURADOR")
    void invite_asIPSFacturador_returns403() throws Exception {
        mockMvc.perform(post("/institutional/members")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_AUDITOR")
    void invite_asIPSAuditor_returns403() throws Exception {
        mockMvc.perform(post("/institutional/members")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_ADMIN")
    void invite_asIPSAdmin_returns201() throws Exception {
        mockMvc.perform(post("/institutional/members")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "IPS_MEDICO")
    void accept_asInvitedUser_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(memberService.accept(eq(id), any())).thenReturn(OrganizationMember.builder()
                .id(id).userId(userId).status(OrganizationMember.MemberStatus.ACTIVE).build());
        User current = User.builder().id(userId).email("m@c.com").role(UserRole.IPS_MEDICO).build();

        mockMvc.perform(post("/institutional/members/{id}/accept", id)
                        .requestAttr(JwtAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE, current))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "IPS_MEDICO")
    void accept_asOtherUser_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(memberService.accept(eq(id), any()))
                .thenThrow(new AccessDeniedException("No puedes aceptar una invitacion ajena"));
        User current = User.builder().id(UUID.randomUUID()).email("other@c.com").role(UserRole.IPS_MEDICO).build();

        mockMvc.perform(post("/institutional/members/{id}/accept", id)
                        .requestAttr(JwtAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE, current))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "IPS_FACTURADOR")
    void assignBranch_asIPSFACTURADOR_returns403() throws Exception {
        mockMvc.perform(put("/institutional/members/{id}/branch/{branchId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
