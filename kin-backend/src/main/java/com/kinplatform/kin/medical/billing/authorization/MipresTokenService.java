package com.kinplatform.kin.medical.billing.authorization;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@Profile("prod")
public class MipresTokenService {

    private final MipresTokenRepository tokenRepository;
    private final WebClient webClient;

    @Value("${mipres.base-url:https://wsmipres.sispro.gov.co/WSMIPRESNOPBS/}")
    private String baseUrl;

    @Value("${mipres.nit:}")
    private String nit;

    @Value("${mipres.pin-base64:}")
    private String pinBase64;

    // In-memory cache with TTL
    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    private record CachedToken(String token, OffsetDateTime expiresAt) {}

    /**
     * Obtiene un token válido para el NIT configurado.
     * Si no hay token válido en cache/BD, genera uno nuevo.
     */
    public String getValidToken() {
        String currentNit = getConfiguredNit();
        CachedToken cached = tokenCache.get(currentNit);

        if (cached != null && !isExpired(cached.expiresAt())) {
            return cached.token();
        }

        // Intentar cargar de BD
        Optional<MipresToken> dbToken = tokenRepository.findByNit(currentNit);
        if (dbToken.isPresent() && !isExpired(dbToken.get().getExpiresAt())) {
            String token = dbToken.get().getToken();
            cacheToken(currentNit, token, dbToken.get().getExpiresAt());
            return token;
        }

        // Generar nuevo token
        return generateAndCacheToken(currentNit);
    }

    /**
     * Genera un nuevo token llamando al endpoint de MinSalud.
     */
    @Transactional
    public String generateAndCacheToken(String nit) {
        String pin = getConfiguredPin();
        String url = baseUrl + "api/GenerarToken/" + nit + "/" + pin;

        log.info("Generando nuevo token MIPRES para NIT: {}", nit);

        try {
            Map<String, Object> response =
                    webClient.post().uri(url).retrieve().bodyToMono(Map.class).block();

            if (response == null || !response.containsKey("token")) {
                throw new MipresTokenException("Respuesta inválida al generar token: " + response);
            }

            String token = (String) response.get("token");
            OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);

            // Guardar en cache
            cacheToken(nit, token, expiresAt);

            // Persistir en BD
            MipresToken tokenEntity = MipresToken.builder()
                    .nit(nit)
                    .token(token)
                    .expiresAt(expiresAt)
                    .build();
            tokenRepository.save(tokenEntity);

            log.info("Token MIPRES generado exitosamente para NIT: {}", nit);
            return token;

        } catch (WebClientResponseException e) {
            log.error("Error HTTP generando token MIPRES: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new MipresTokenException("Error generando token MIPRES: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error generando token MIPRES", e);
            throw new MipresTokenException("Error generando token MIPRES: " + e.getMessage());
        }
    }

    /**
     * Renueva el token si está próximo a expirar (ejecutado cada 23h).
     */
    @Scheduled(fixedDelayString = "${mipres.token-renewal-interval-ms:82800000}") // 23 horas = 82800000ms
    public void renewTokenIfNeeded() {
        String nit = getConfiguredNit();
        CachedToken cached = tokenCache.get(nit);

        if (cached != null && isExpiringSoon(cached.expiresAt())) {
            log.info("Renovando token MIPRES proactivamente para NIT: {}", nit);
            try {
                generateAndCacheToken(nit);
            } catch (Exception e) {
                log.error("Error renovando token MIPRES proactivamente", e);
            }
        }
    }

    private void cacheToken(String nit, String token, OffsetDateTime expiresAt) {
        tokenCache.put(nit, new CachedToken(token, expiresAt));
    }

    private boolean isExpired(OffsetDateTime expiresAt) {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    private boolean isExpiringSoon(OffsetDateTime expiresAt) {
        return OffsetDateTime.now().plusHours(1).isAfter(expiresAt); // 1 hora antes
    }

    private String getConfiguredNit() {
        if (nit == null || nit.isBlank()) {
            throw new IllegalStateException("MIPRES_NIT no configurado. Configure la variable de entorno MIPRES_NIT.");
        }
        return nit;
    }

    private String getConfiguredPin() {
        if (pinBase64 == null || pinBase64.isBlank()) {
            throw new IllegalStateException(
                    "MIPRES_PIN_BASE64 no configurado. Configure la variable de entorno MIPRES_PIN_BASE64.");
        }
        return pinBase64;
    }

    public static class MipresTokenException extends RuntimeException {
        public MipresTokenException(String message) {
            super(message);
        }
    }
}
