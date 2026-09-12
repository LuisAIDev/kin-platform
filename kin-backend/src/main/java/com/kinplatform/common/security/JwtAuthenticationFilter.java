package com.kinplatform.common.security;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String TOKEN_COOKIE = "kin_token_v2";

    public static final String AUTHENTICATED_USER_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".authenticatedUser";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token == null) {
            token = extractCookieToken(request);
        }

        if (token == null) {
            log.debug("No Bearer token or session cookie found for URI={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            log.warn("Invalid or expired token for URI={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        var email = jwtService.extractEmail(token);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Token válido pero usuario inexistente en BD para email={}, URI={}", email, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != UserRole.PHYSICIAN && user.getRole() != UserRole.PATIENT) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }
        if (PhysicianAccess.isPhysician(user)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PHYSICIAN"));
        }
        if (PatientAccess.isPatient(user)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PATIENT"));
        }

        request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);

        var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Aislamiento por plataforma. NOTA: el backend se sirve bajo
        // /api/v1 (context-path), y getRequestURI() lo incluye; por eso el
        // predicado recibe la ruta SIN normalizar y el aislamiento queda
        // INACTIVO (comportamiento actual de producción). Activar el
        // aislamiento exige normalizar la ruta y reconciliar ADR-039/040.
        if (!PlatformAccess.canAccess(user.getPlatform(), user.getRole().name(), request.getRequestURI())) {
            log.warn("Acceso cruzado bloqueado: platform={}, role={} no puede acceder a URI={}",
                    user.getPlatform(), user.getRole().name(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso prohibido: plataforma no autorizada para esta ruta");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String extractCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
