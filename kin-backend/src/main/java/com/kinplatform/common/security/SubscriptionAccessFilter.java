package com.kinplatform.common.security;

import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    private final SubscriptionValidatorService validatorService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth")
            || path.startsWith("/pricing-plans")
            || path.startsWith("/actuator")
            || path.startsWith("/subscriptions")
            || "GET".equalsIgnoreCase(request.getMethod())
            || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = user.getId();
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (isProjectCreation(method, path) && !validatorService.canCreateProject(userId)) {
            log.warn("Usuario {} bloqueado: límite de proyectos alcanzado", userId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Límite de proyectos alcanzado. Actualiza tu plan para crear más proyectos.\"}");
            return;
        }

        if (isMessageSend(method, path) && !validatorService.canSendMessage(userId)) {
            log.warn("Usuario {} bloqueado: límite de mensajes alcanzado", userId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Límite de mensajes mensual alcanzado. Espera al próximo período o actualiza tu plan.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProjectCreation(String method, String path) {
        return "POST".equalsIgnoreCase(method) && path.matches(".*/projects/?$");
    }

    private boolean isMessageSend(String method, String path) {
        return "POST".equalsIgnoreCase(method) && path.matches(".*/projects/[^/]+/chat/?$");
    }
}
