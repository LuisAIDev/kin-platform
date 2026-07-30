package com.kinplatform.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("=== JWT FILTER === method={}, requestURI={}, servletPath={}, contextPath={}, hasAuthorization={}, authenticatedUser={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getServletPath(),
                request.getContextPath(),
                authHeader != null,
                auth != null ? auth.getName() : "none");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("=== JWT FILTER === No Bearer token found for URI={}. auth after skip: {}",
                    request.getRequestURI(),
                    SecurityContextHolder.getContext().getAuthentication() != null
                            ? SecurityContextHolder.getContext().getAuthentication().getName()
                            : "null");
            filterChain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            log.warn("=== JWT FILTER === Token INVALID for URI={}. Token (first 20): {}...",
                    request.getRequestURI(),
                    token.substring(0, Math.min(20, token.length())));
            filterChain.doFilter(request, response);
            return;
        }

        var email = jwtService.extractEmail(token);
        var role = jwtService.extractRole(token);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = new UsernamePasswordAuthenticationToken(
                email, null, authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("=== JWT FILTER === AUTHENTICATION SET for URI={}, user={}, role={}, authorities={}",
                request.getRequestURI(), email, role, authorities);
        filterChain.doFilter(request, response);
    }
}
