package com.kinplatform.common.controller;

import com.kinplatform.common.config.SecurityConfig;
import com.kinplatform.common.entity.PrivacyPolicyVersion;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import com.kinplatform.common.service.PrivacyPolicyService;
import com.kinplatform.kin.health.hce.controller.EncounterController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión del bug de doble prefijo con {@code server.servlet.context-path=/api/v1}.
 *
 * <p>Spring Security y Spring MVC matchean contra el <b>servlet path</b> (sin
 * context-path). Por tanto los {@code @RequestMapping} y los matchers de
 * {@code SecurityConfig} NUNCA deben incluir {@code /api/v1}: el context-path
 * ya lo aporta. Si un controller lo incluye, la URL real se duplica
 * ({@code /api/v1/api/v1/...}) y en producción devuelve 404/403.</p>
 *
 * <p>Este test opera en un slice {@code @WebMvcTest} (sin context-path, sin BD),
 * que es exactamente donde el bug se vuelve visible: el mapping correcto es
 * {@code /public/privacy-policy} y el path duplicado {@code /api/v1/public/...}
 * no debe resolver. Mismo patrón que {@code BillingEndpointPathTest}.</p>
 */
@WebMvcTest(controllers = PrivacyPolicyController.class)
@Import(SecurityConfig.class)
class ContextPathRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrivacyPolicyService privacyPolicyService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @BeforeEach
    void setUp() throws Exception {
        PrivacyPolicyVersion active = PrivacyPolicyVersion.builder()
                .id(java.util.UUID.randomUUID())
                .version("1.0")
                .title("Política de Privacidad v1.0")
                .contentMd("# v1.0")
                .active(true)
                .build();
        when(privacyPolicyService.getActivePolicy()).thenReturn(Optional.of(active));
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
    void publicPolicy_mapsWithoutApiV1Prefix_returns200() throws Exception {
        mockMvc.perform(get("/public/privacy-policy"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    void doubledApiV1Prefix_doesNotResolve_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/privacy-policy"))
                .andExpect(status().isNotFound());
    }

    /**
     * Canary de TD-API-1: los 16 controllers restantes (HCE, Consent,
     * DataRectification/Export/Deletion) aún declaran {@code /api/v1} en su
     * {@code @RequestMapping} y están rotos en producción. Este test DEBE
     * fallar hasta completar la Opción B.
     */
    @Test
    @Disabled("TD-API-1: falla hasta corregir los 16 controllers restantes (HCE/Consent/Data-*)")
    void hceControllers_mustNotDeclareApiV1InMapping() {
        RequestMapping mapping = EncounterController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value())
                .as("Los @RequestMapping no deben incluir /api/v1 (lo aporta el context-path)")
                .noneMatch(path -> path.startsWith("/api/v1"));
    }
}
