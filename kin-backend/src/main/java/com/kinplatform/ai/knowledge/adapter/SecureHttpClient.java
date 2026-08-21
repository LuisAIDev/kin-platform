package com.kinplatform.ai.knowledge.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Cliente HTTP real y seguro (ADR-021) que implementa el seam de
 * {@link HttpKnowledgeSourceAdapter.HttpClient} sin romper el contrato del
 * adaptador ni el dominio.
 *
 * <p>Protecciones implementadas en Java (el LLM nunca decide destinos):</p>
 * <ul>
 *   <li>validación SSRF de cada URL y de cada salto de redirección mediante
 *       {@link SourceConnectionGuard} (allowlist + bloqueo de IPs privadas +
 *       resolución DNS fail-closed);</li>
 *   <li>connect timeout y request timeout acotados (nunca bloquea el pipeline
 *       indefinidamente);</li>
 *   <li>límite de tamaño de respuesta (aborta si se supera);</li>
 *   <li>límite de redirecciones (3 por defecto);</li>
 *   <li>control de content-type cuando la allowlist de tipos no está vacía;</li>
 *   <li>retry acotado y con backoff fijo únicamente para fallos seguros de
 *       reejecutar (errores de conexión y 5xx), nunca 4xx ni tras éxito;</li>
 *   <li>offline-first: ningún error escapa, se devuelve una respuesta marcada
 *       como fallo y el pipeline degrada con gracia.</li>
 * </ul>
 */
public class SecureHttpClient implements HttpKnowledgeSourceAdapter.HttpClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecureHttpClient.class);

    private final HttpClient client;
    private final SourceConnectionGuard guard;
    private final String sourceId;
    private final Duration requestTimeout;
    private final long maxResponseBytes;
    private final int maxRedirects;
    private final Set<String> allowedContentTypes;
    private final int retries;
    private final Duration retryBackoff;
    private final KnowledgeAdapterMetrics metrics;

    public SecureHttpClient(
            HttpClient client,
            SourceConnectionGuard guard,
            String sourceId,
            Duration requestTimeout,
            long maxResponseBytes,
            int maxRedirects,
            Set<String> allowedContentTypes,
            int retries,
            Duration retryBackoff,
            KnowledgeAdapterMetrics metrics) {
        this.client = client == null ? HttpClient.newBuilder().build() : client;
        this.guard = guard == null ? new SourceConnectionGuard(Set.of(), false) : guard;
        this.sourceId = sourceId == null ? "" : sourceId;
        this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(3) : requestTimeout;
        this.maxResponseBytes = Math.max(1, maxResponseBytes);
        this.maxRedirects = Math.max(0, maxRedirects);
        this.allowedContentTypes = normalizeContentTypes(allowedContentTypes);
        this.retries = Math.max(0, Math.min(3, retries));
        this.retryBackoff = retryBackoff == null ? Duration.ofMillis(250) : retryBackoff;
        this.metrics = metrics == null ? new KnowledgeAdapterMetrics(null) : metrics;
    }

    @Override
    public HttpKnowledgeSourceAdapter.HttpResponse fetch(HttpKnowledgeSourceAdapter.HttpRequest request) {
        if (request == null) {
            return null;
        }
        String url = request.url();
        SourceConnectionGuard.Validation validation = guard.validate(url);
        if (!validation.allowed()) {
            metrics.rejected(sourceId);
            return null;
        }
        metrics.request(sourceId);
        long start = System.nanoTime();
        try {
            HttpKnowledgeSourceAdapter.HttpResponse response = executeWithRetries(validation.uri());
            if (response != null) {
                metrics.latency(sourceId, elapsedMs(start));
                if (isSuccess(response.statusCode())) {
                    metrics.success(sourceId);
                } else {
                    metrics.failure(sourceId);
                }
            }
            return response;
        } catch (IOException | InterruptedException ex) {
            metrics.failure(sourceId);
            Thread.currentThread().interrupt();
            return null;
        } catch (RuntimeException ex) {
            metrics.failure(sourceId);
            return null;
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status <= 299;
    }

    private HttpKnowledgeSourceAdapter.HttpResponse executeWithRetries(URI uri)
            throws IOException, InterruptedException {
        int attempts = retries + 1;
        HttpKnowledgeSourceAdapter.HttpResponse last = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(retryBackoff.toMillis());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
            HttpKnowledgeSourceAdapter.HttpResponse response = executeOnce(uri);
            if (response != null) {
                int status = response.statusCode();
                if (status >= 200 && status <= 299) {
                    return response;
                }
                last = response;
                if (status >= 400 && status <= 499) {
                    return response; // 4xx no se reintenta
                }
            }
        }
        return last;
    }

    private HttpKnowledgeSourceAdapter.HttpResponse executeOnce(URI uri) throws IOException, InterruptedException {
        URI current = uri;
        int redirects = 0;
        while (redirects <= maxRedirects) {
            SourceConnectionGuard.Validation validation = guard.validate(current.toString());
            if (!validation.allowed()) {
                metrics.rejected(sourceId);
                return null;
            }
            HttpRequest httpRequest = HttpRequest.newBuilder(validation.uri())
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<InputStream> raw;
            try {
                raw = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            } catch (java.net.http.HttpTimeoutException ex) {
                metrics.timeout(sourceId);
                return null;
            } catch (java.net.ConnectException ex) {
                metrics.failure(sourceId);
                return null;
            } catch (IOException ex) {
                throw ex;
            }
            int status = raw.statusCode();
            if (status == 429) {
                // Rate limit de la fuente (p. ej. throttling anónimo por IP de
                // datos.gov.co): métrica dedicada + WARN para la observación.
                metrics.rateLimited(sourceId);
                if (log.isWarnEnabled()) {
                    log.warn("Source '{}' responded HTTP 429 (rate limit) for URI={}", sourceId, current);
                }
            }
            if (status >= 300 && status <= 399) {
                String location = raw.headers().firstValue("Location").orElse(null);
                if (location == null) {
                    return new HttpKnowledgeSourceAdapter.HttpResponse(status, contentType(raw), "");
                }
                if (++redirects > maxRedirects) {
                    metrics.rejected(sourceId);
                    return null;
                }
                current = current.resolve(location);
                continue;
            }
            if (status >= 200 && status <= 299 && !contentTypeAllowed(raw)) {
                metrics.rejected(sourceId);
                return new HttpKnowledgeSourceAdapter.HttpResponse(status, contentType(raw), "");
            }
            String body = status >= 200 && status <= 299 ? readLimited(raw) : "";
            if (body == null) {
                metrics.timeout(sourceId);
                return new HttpKnowledgeSourceAdapter.HttpResponse(0, contentType(raw), "");
            }
            return new HttpKnowledgeSourceAdapter.HttpResponse(status, contentType(raw), body);
        }
        metrics.rejected(sourceId);
        return null;
    }

    private boolean contentTypeAllowed(HttpResponse<InputStream> raw) {
        if (allowedContentTypes.isEmpty()) {
            return true;
        }
        String type = contentType(raw);
        if (type == null || type.isBlank()) {
            return false;
        }
        // Normaliza igual que la allowlist (se ignora el parámetro charset):
        // "application/json; charset=utf-8" -> "application/json".
        String normalized = type.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (allowedContentTypes.contains(normalized)) {
            return true;
        }
        // Acepta sub-tipos +json (p. ej. "application/vnd.sdmx.data+json") cuando
        // "application/json" está permitido (ECB, APIs SDMX-JSON, problem+json).
        return allowedContentTypes.contains("application/json")
                && normalized.startsWith("application/")
                && normalized.endsWith("+json");
    }

    private String contentType(HttpResponse<InputStream> raw) {
        return raw.headers().firstValue("Content-Type").orElse("");
    }

    /**
     * Lee el cuerpo limitado a {@code maxResponseBytes}. Devuelve {@code null}
     * si el cuerpo supera el límite (se aborta la lectura) para tratar el exceso
     * como rechazo controlado.
     */
    private String readLimited(HttpResponse<InputStream> raw) {
        try (InputStream stream = raw.body()) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;
            while ((read = stream.read(chunk)) != -1) {
                total += read;
                if (total > maxResponseBytes) {
                    return null;
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private Set<String> normalizeContentTypes(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new java.util.LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toLowerCase(Locale.ROOT).split(";")[0]);
            }
        }
        return Set.copyOf(out);
    }
}
