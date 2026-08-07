package com.kinplatform.common.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityConfigCorsTest {

    @Test
    void corsConfigurationSource_deberiaSerLaUnicaFuenteDeCors() {
        var config = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(RateLimitingFilter.class),
                mock(SubscriptionAccessFilter.class));
        ReflectionTestUtils.setField(
                config, "allowedOrigins", "http://localhost:3000,https://kin-frontend.onrender.com");

        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(cors);
        assertTrue(cors.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(cors.getAllowedOrigins().contains("https://kin-frontend.onrender.com"));
        assertTrue(cors.getAllowedOrigins().contains("https://kin-platform.vercel.app"));
        assertTrue(cors.getAllowCredentials());
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
    }

    @Test
    void corsConfigurationSource_sinOrigenesConfigurados_deberiaIncluirProduccion() {
        var config = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(RateLimitingFilter.class),
                mock(SubscriptionAccessFilter.class));
        ReflectionTestUtils.setField(config, "allowedOrigins", "");

        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertTrue(cors.getAllowedOrigins().contains("https://kin-platform.vercel.app"));
    }
}
