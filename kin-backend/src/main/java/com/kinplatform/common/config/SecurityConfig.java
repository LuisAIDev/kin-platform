package com.kinplatform.common.config;

import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final com.kinplatform.common.security.SubscriptionAccessFilter subscriptionAccessFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

/**
 * Orígenes de frontend que nunca se pierden, incluso si ALLOWED_ORIGINS
 * está definido sin incluirlos. KIN Platform utiliza el dominio propio:
 * https://www.kin-platform-medical.com y https://kin-platform.com.
 * No se permite *.vercel.app genérico (riesgo de acceso no autorizado).
 */
    private static final List<String> GUARANTEED_ORIGINS = List.of(
        "https://www.kin-platform-medical.com",
        "https://kin-platform-medical.com",
        "https://kin-platform.com",
        "https://www.kin-platform.com",
        "https://kin-frontend-medical.vercel.app",
        "https://kin-frontend.vercel.app"
    );

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/auth/**")
                        .permitAll()
                        .requestMatchers("/pricing-plans/**")
                        .permitAll()
                        .requestMatchers("/stripe/webhook")
                        .permitAll()
                        // Webhook de Wompi (server-to-server): sin autenticación JWT;
                        // la autenticidad se valida con el checksum X-Event-Checksum.
                        .requestMatchers("/wompi/webhook")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/knowledge/**")
                        .hasRole("ADMIN")
                        // Enlace público de compartición de triaje (médico externo sin cuenta).
                        // Debe ir ANTES de /health/triage/** (que exige rol).
                        .requestMatchers("/health/triage/share/**")
                        .permitAll()
                        .requestMatchers("/health/triage/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "ADMIN")
                        .requestMatchers("/health/differential/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "ADMIN")
                        .requestMatchers("/health/dashboard/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "ADMIN")
                        .requestMatchers("/health/patient/consent/**")
                        .authenticated()
                        // Gestión de invitaciones recibidas: accesible a cualquier usuario
                        // autenticado que sea la parte invitada (aislamiento por identidad en
                        // el servicio). Antes de aceptar el consentimiento (PENDING_CONSENT) el
                        // usuario aún no tiene ROLE_PATIENT, pero debe poder ver/aceptar su
                        // invitación. Debe ir ANTES de /health/patient/**.
                        .requestMatchers("/health/patient/relationships/**")
                        .authenticated()
                        .requestMatchers("/health/patient/**")
                        .hasAnyRole("PATIENT", "ADMIN")
                        // Solicitud de capacidad profesional (Alternativa B): accesible para
                        // CUALQUIER usuario autenticado (FREE/PREMIUM/PATIENT existentes),
                        // no solo PHYSICIAN. Debe ir ANTES de /health/physician/**.
                        .requestMatchers("/health/physician/application/**")
                        .authenticated()
                        .requestMatchers("/health/physician/**")
                        .hasAnyRole("PHYSICIAN", "ADMIN")
                        .requestMatchers("/health/notifications/**")
                        .hasAnyRole("PATIENT", "PHYSICIAN")
                        .requestMatchers("/health/followup/**")
                        .hasAnyRole("PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/health/scheduling/**")
                        .hasAnyRole("PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/health/audit/**")
                        .hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/health/documents/**")
                        .hasAnyRole("PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/health/automation/**")
                        .hasAnyRole("PHYSICIAN", "ADMIN")
                        .requestMatchers("/health/aiassist/**")
                        .hasAnyRole("PHYSICIAN", "PATIENT", "ADMIN")
                        .requestMatchers("/health/telemedicine/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/empresas/enterprise/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "ADMIN")
                        .requestMatchers("/empresas/projects/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "ADMIN")
                        .requestMatchers("/empresas/categories/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "ADMIN")
                        .requestMatchers("/medical/telemedicine/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/medical/documents/**")
                        .hasAnyRole("PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/medical/dashboard/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "ADMIN")
                        .requestMatchers("/medical/scheduling/**")
                        .hasAnyRole("PATIENT", "PHYSICIAN", "ADMIN")
                        .requestMatchers("/medical/differential/**")
                        .hasAnyRole("FREE", "PREMIUM", "FACILITADOR", "PATIENT", "ADMIN")
                        .requestMatchers("/medical/audit/**")
                        .hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/medical/automation/**")
                        .hasAnyRole("PHYSICIAN", "ADMIN")
                        .requestMatchers("/medical/aiassist/**")
                        .hasAnyRole("PHYSICIAN", "PATIENT", "ADMIN")
                        // Facturacion RIPS / FEV: solo personal de la IPS (back-office).
                        // No existe rol IPS_* aun; se usa el fallback ADMIN/PHYSICIAN.
                        .requestMatchers("/api/v1/billing/contracts/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/billing/authorizations/**")
                        .hasAnyRole("PHYSICIAN", "ADMIN")
                        .requestMatchers("/api/v1/billing/fev-rips/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/billing/rips/**")
                        .hasAnyRole("PHYSICIAN", "ADMIN")
                        .requestMatchers("/api/v1/billing/**")
                        .hasAnyRole("PHYSICIAN", "ADMIN")
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers("/test/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/subscriptions/**")
                        .authenticated()
                        .requestMatchers("/stripe/create-checkout-session")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .headers(headers -> headers.contentSecurityPolicy(
                                csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(
                                hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                        .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "strict-origin-when-cross-origin")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(subscriptionAccessFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var origins = new ArrayList<>(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        // Garantía de producción: los orígenes del frontend nunca se pierden,
        // incluso si ALLOWED_ORIGINS está definido sin incluirlos.
        for (String origin : GUARANTEED_ORIGINS) {
            if (!origins.contains(origin)) {
                origins.add(origin);
            }
        }
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
