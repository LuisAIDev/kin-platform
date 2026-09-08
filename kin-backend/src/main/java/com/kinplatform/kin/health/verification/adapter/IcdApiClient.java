package com.kinplatform.kin.health.verification.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.verification.WhoVerificationProperties;
import com.kinplatform.kin.health.verification.domain.DiagnosticMatch;
import com.kinplatform.kin.health.verification.port.IcdDiagnosisLookup;
import com.kinplatform.kin.health.verification.port.WhoHealthDataSourceException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Cliente de la ICD-API de la OMS (versión 2).
 *
 * <p>Autenticación OAuth2 Client Credentials: obtiene un token de acceso en el
 * token endpoint de la OMS ({@code https://icdaccessmanagement.who.int/connect/token}),
 * lo cachea en memoria hasta su expiración y lo usa en las búsquedas. Todas las
 * llamadas llevan la cabecera {@code API-Version: v2} (contrato v2 de la OMS).</p>
 *
 * <p>Sin credenciales configuradas el cliente no se usa: la fachada
 * {@code WhoVerificationService} devuelve {@code UNCONFIGURED} y la IA queda
 * instruida para no inventar códigos.</p>
 */
@Component
public class IcdApiClient implements IcdDiagnosisLookup {

    private static final Logger log = LoggerFactory.getLogger(IcdApiClient.class);

    private final WhoVerificationProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private volatile String cachedAccessToken = "";
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public IcdApiClient(WhoVerificationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props().getConnectTimeoutMillis());
        factory.setReadTimeout(props().getReadTimeoutMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public List<DiagnosticMatch> search(String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        String token = accessToken();
        String encoded = URLEncoder.encode(term.trim(), StandardCharsets.UTF_8);
        String url = icdBaseUrl()
                + "/icd/release/11/" + props().getReleaseId() + "/" + props().getLinearization()
                + "/search?q=" + encoded + "&useFlexibleSearch=true";

        try {
            String body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("API-Version", props().getApiVersion())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            return parse(body, term);
        } catch (Exception e) {
            throw new WhoHealthDataSourceException("ICD-API no disponible: " + e.getMessage(), e);
        }
    }

    private List<DiagnosticMatch> parse(String body, String term) {
        List<DiagnosticMatch> matches = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return matches;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode entities = root.path("destinationEntities");
            int max = props().getMaxResults();
            for (JsonNode entity : entities) {
                if (matches.size() >= max) {
                    break;
                }
                String code = firstText(entity, "code", "theCode");
                String title = firstText(entity, "title", "label", "prefLabel", "label@value");
                if (code.isBlank() && title.isBlank()) {
                    continue;
                }
                matches.add(new DiagnosticMatch(code, title, term, props().getReleaseId()));
            }
        } catch (Exception e) {
            log.warn("IcdApiClient: no se pudo parsear la respuesta de búsqueda: {}", e.getMessage());
        }
        return matches;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null) {
                continue;
            }
            if (value.isTextual()) {
                return value.asText();
            }
            if (value.isObject()) {
                JsonNode text = value.get("@value");
                if (text != null && text.isTextual()) {
                    return text.asText();
                }
            }
        }
        return "";
    }

    /** Token de acceso OAuth2, cacheado hasta su expiración. */
    private String accessToken() {
        if (!cachedAccessToken.isBlank() && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }
        synchronized (this) {
            if (!cachedAccessToken.isBlank() && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedAccessToken;
            }
            fetchToken();
            return cachedAccessToken;
        }
    }

    private void fetchToken() {
        WhoVerificationProperties.Icd icd = props();
        if (isBlank(icd.getClientId()) || isBlank(icd.getClientSecret())) {
            throw new WhoHealthDataSourceException(
                    "ICD-API sin credenciales OAuth2 (WHO_ICD_CLIENT_ID/WHO_ICD_CLIENT_SECRET)", null);
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("scope", icd.getScope());
            form.add("client_id", icd.getClientId());
            form.add("client_secret", icd.getClientSecret());
            String body = restClient.post()
                    .uri(icd.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            String token = root.path("access_token").asText("");
            long expiresIn = root.path("expires_in").asLong(3600);
            if (token.isBlank()) {
                throw new WhoHealthDataSourceException("ICD-API no devolvió access_token", null);
            }
            cachedAccessToken = token;
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(1, expiresIn - 30));
            log.info("IcdApiClient: token OAuth2 renovado (expira en {}s)", expiresIn);
        } catch (WhoHealthDataSourceException e) {
            throw e;
        } catch (Exception e) {
            throw new WhoHealthDataSourceException("No se pudo obtener el token OAuth2 de la ICD-API", e);
        }
    }

    private String icdBaseUrl() {
        String base = props().getBaseUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private WhoVerificationProperties.Icd props() {
        return properties.getIcd();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
