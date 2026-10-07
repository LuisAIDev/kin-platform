package com.kinplatform.common.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de integración del {@link SecureHttpClient} sobre un servidor HTTP
 * local determinista (JDK {@link HttpServer} en loopback, sin Internet).
 */
class SecureHttpClientTest {

    private HttpServer server;
    private int port;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private KnowledgeAdapterMetrics metrics;
    private HttpClient jdkClient;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        port = server.getAddress().getPort();
        metrics = new KnowledgeAdapterMetrics(registry);
        jdkClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handle(int status, String contentType, String body) {
        server.createContext("/search", exchange -> respond(exchange, status, contentType, body));
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        if (body == null) {
            body = "";
        }
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private SecureHttpClient client(SourceConnectionGuard guard, long maxBytes, int retries) {
        return new SecureHttpClient(
                jdkClient,
                guard,
                "src-http",
                Duration.ofMillis(500),
                maxBytes,
                3,
                Set.of("application/json"),
                retries,
                Duration.ofMillis(50),
                metrics);
    }

    private SourceConnectionGuard loopbackGuard() {
        return new SourceConnectionGuard(Set.of(), true);
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/search";
    }

    private HttpKnowledgeSourceAdapter.HttpRequest request() {
        return new HttpKnowledgeSourceAdapter.HttpRequest(
                baseUrl() + "?q=retail", Map.of("Accept", "application/json"));
    }

    @Test
    void happyPath_deberiaDevolverRespuestaConCuerpo() throws Exception {
        handle(200, "application/json", "{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\"}]}");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals("{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\"}]}", response.body());
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.requests", "src-http"));
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.success", "src-http"));
    }

    @Test
    void http500_deberiaDevolverFalloControladoSinRetryInfinito() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/search", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 500, "application/json", "{}");
        });

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 1).fetch(request());

        assertEquals(500, response.statusCode());
        assertEquals(2, calls.get(), "1 intento + 1 retry acotado");
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.failure", "src-http"));
    }

    @Test
    void http429_rateLimit_deberiaRegistrarMetricaDedicada() throws Exception {
        handle(429, "application/json", "{}");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(429, response.statusCode());
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.rate_limited", "src-http"));
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.failure", "src-http"));
    }

    @Test
    void http404_noDeberiaReintentarse() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/search", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 404, "application/json", "{}");
        });

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 1).fetch(request());

        assertEquals(404, response.statusCode());
        assertEquals(1, calls.get());
    }

    @Test
    void retry_transitorio500DeberiaRecuperarse() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/search", exchange -> {
            if (calls.incrementAndGet() == 1) {
                respond(exchange, 500, "application/json", "{}");
            } else {
                respond(exchange, 200, "application/json", "{\"items\":[]}");
            }
        });

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 1).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals(2, calls.get());
    }

    @Test
    void timeout_deberiaDegradarSinRomper() throws Exception {
        server.createContext("/search", exchange -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "application/json", "{}");
        });

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertNull(response);
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.timeout", "src-http"));
    }

    @Test
    void excesoDeTamano_deberiaRechazarse() throws Exception {
        String big = "x".repeat(10_000);
        handle(200, "application/json", "{\"items\":[{\"content\":\"" + big + "\"}]}");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(0, response.statusCode());
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.timeout", "src-http"));
    }

    @Test
    void contentTypeConCharset_deberiaAceptarse() throws Exception {
        handle(
                200,
                "application/json; charset=utf-8",
                "{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\"}]}");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals("{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\"}]}", response.body());
    }

    @Test
    void contentTypeVendorJson_deberiaAceptarse() throws Exception {
        handle(
                200,
                "application/vnd.sdmx.data+json;version=1.0.0; charset=utf-8",
                "{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\"}]}");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals("{\"items\":[{\"content\":\"dato\",\"url\":\"https://a.com/1\"}]}", response.body());
    }

    @Test
    void contentTypeNoPermitido_deberiaRechazarse() throws Exception {
        handle(200, "text/html", "<html></html>");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals("", response.body());
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.rejected", "src-http"));
    }

    @Test
    void redireccionAHostNoPermitido_deberiaRechazarse() throws Exception {
        server.createContext("/search", exchange -> {
            exchange.getResponseHeaders().add("Location", "https://evil.com/x");
            respond(exchange, 302, "application/json", "");
        });

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertNull(response);
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.rejected", "src-http"));
    }

    @Test
    void redireccionDentroDeDestinoPermitido_deberiaSeguirse() throws Exception {
        server.createContext("/search", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:" + port + "/target");
            respond(exchange, 302, "application/json", "");
        });
        server.createContext(
                "/target",
                exchange -> respond(
                        exchange,
                        200,
                        "application/json",
                        "{\"items\":[{\"content\":\"ok\",\"url\":\"https://a.com/1\"}]}"));

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals("{\"items\":[{\"content\":\"ok\",\"url\":\"https://a.com/1\"}]}", response.body());
    }

    @Test
    void destinoNoPermitidoPorAllowlist_deberiaRechazarseSinConectar() {
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("autorizado.com"), false);
        HttpKnowledgeSourceAdapter.HttpRequest request =
                new HttpKnowledgeSourceAdapter.HttpRequest("https://malicioso.com/x", Map.of());

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(guard, 1024, 0).fetch(request);

        assertNull(response);
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.rejected", "src-http"));
    }

    @Test
    void cuerpoVacio_conDecoderJSON_deberiaProducirListaVacia() throws Exception {
        handle(200, "application/json", "");

        HttpKnowledgeSourceAdapter.HttpResponse response =
                client(loopbackGuard(), 1024, 0).fetch(request());

        assertEquals(200, response.statusCode());
        assertEquals("", response.body());
    }
}

