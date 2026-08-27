package com.kinplatform.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Desbloqueo de rate limiting (solo ADMIN, vía {@code SecurityConfig}).
 *
 * <p>{@code POST /admin/security/rate-limit/reset?ip=...} limpia los buckets
 * de la IP indicada (o de la IP de la propia solicitud si se omite). Útil para
 * desbloquear a usuarios legítimos sin esperar la expiración de la ventana.</p>
 */
@RestController
@RequestMapping("/admin/security/rate-limit")
@RequiredArgsConstructor
public class RateLimitAdminController {

    private final RateLimitingFilter rateLimitingFilter;

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(
            @RequestParam(value = "ip", required = false) String ip, HttpServletRequest request) {
        String targetIp = (ip == null || ip.isBlank()) ? rateLimitingFilter.resolveClientIp(request) : ip.trim();
        int cleared = rateLimitingFilter.reset(targetIp);
        return ResponseEntity.ok(Map.of("ip", targetIp, "cleared", cleared));
    }
}
