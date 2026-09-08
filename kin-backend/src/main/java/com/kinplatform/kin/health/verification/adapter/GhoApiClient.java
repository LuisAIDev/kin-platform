package com.kinplatform.kin.health.verification.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.verification.WhoVerificationProperties;
import com.kinplatform.kin.health.verification.domain.GlobalStatistic;
import com.kinplatform.kin.health.verification.port.GlobalHealthStatProvider;
import com.kinplatform.kin.health.verification.port.WhoHealthDataSourceException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente del Global Health Observatory (GHO) de la OMS.
 *
 * <p>El GHO expone indicadores de salud poblacional (mortalidad, incidencia,
 * prevalencia…) sin autenticación. Se usa para dar contexto numérico oficial a
 * las respuestas cuando hay indicadores configurados
 * ({@code kin.health.verification.gho.indicators}).</p>
 */
@Component
public class GhoApiClient implements GlobalHealthStatProvider {

    private static final Logger log = LoggerFactory.getLogger(GhoApiClient.class);

    private final WhoVerificationProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GhoApiClient(WhoVerificationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getGho().getConnectTimeoutMillis());
        factory.setReadTimeout(properties.getGho().getReadTimeoutMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public List<GlobalStatistic> indicator(String indicatorCode, String countryCode) {
        if (indicatorCode == null || indicatorCode.isBlank()) {
            return List.of();
        }
        StringBuilder url = new StringBuilder(ghoBaseUrl()).append('/').append(indicatorCode.trim())
                .append("?format=json");
        if (countryCode != null && !countryCode.isBlank()) {
            url.append("&filter=COUNTRY:").append(countryCode.trim());
        }
        try {
            String body = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            return parse(body, indicatorCode);
        } catch (Exception e) {
            throw new WhoHealthDataSourceException("GHO no disponible: " + e.getMessage(), e);
        }
    }

    private List<GlobalStatistic> parse(String body, String indicatorCode) {
        List<GlobalStatistic> stats = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return stats;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode facts = root.path("fact");
            for (JsonNode fact : facts) {
                JsonNode dim = fact.path("dim");
                String country = firstText(dim, "COUNTRY", "REGION");
                String year = firstText(dim, "YEAR");
                String value = stringValue(fact.path("value"));
                if (value.isBlank()) {
                    continue;
                }
                stats.add(new GlobalStatistic(
                        indicatorCode, "", country, value, year));
            }
        } catch (Exception e) {
            log.warn("GhoApiClient: no se pudo parsear la respuesta del indicador {}: {}", indicatorCode,
                    e.getMessage());
        }
        return stats;
    }

    private static String stringValue(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return "";
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return "";
    }

    private String ghoBaseUrl() {
        String base = properties.getGho().getBaseUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
