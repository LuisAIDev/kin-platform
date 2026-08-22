package com.kinplatform.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integración del {@link RedisKnowledgeRepository} con Redis real
 * (Testcontainers, sin Internet): hit/miss, TTL, claves deterministas y
 * aislamiento por consulta (ADR-021).
 */
@Testcontainers
class RedisKnowledgeRepositoryTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static StringRedisTemplate redis;
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final KnowledgeAdapterMetrics metrics = new KnowledgeAdapterMetrics(registry);

    @BeforeAll
    static void setUpRedis() {
        var factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        if (redis != null && redis.getConnectionFactory() != null) {
            ((LettuceConnectionFactory) redis.getConnectionFactory()).destroy();
        }
    }

    private RedisKnowledgeRepository repository() {
        return new RedisKnowledgeRepository(redis, new ObjectMapper(), metrics);
    }

    private static KnowledgeQuery query(String topic, String... keywords) {
        return KnowledgeQuery.from(KnowledgeRequest.of(topic, List.of(keywords)));
    }

    private static KnowledgeResult result(String claim, String sourceId, String url) {
        var fact = new KnowledgeFact(
                null,
                claim,
                sourceId,
                url,
                OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5)),
                SourceTrust.OFFICIAL_PUBLIC,
                "MARKET");
        return new KnowledgeResult(
                List.of(fact), List.of(sourceId), List.of(), 0.9, "verificado", "KnowledgeEngine", "v1");
    }

    private static KnowledgeFact factWithMaxAge(String claim, String sourceId, Duration maxAge) {
        var fact = new KnowledgeFact(
                null,
                claim,
                sourceId,
                "https://data.autorizado.com/" + sourceId,
                OffsetDateTime.now().minusDays(1),
                SourceTrust.OFFICIAL_PUBLIC,
                "MARKET");
        return new KnowledgeFact(
                fact.id(),
                fact.claim(),
                fact.sourceId(),
                fact.url(),
                fact.publishedAt(),
                fact.trust(),
                fact.category(),
                maxAge);
    }

    private static KnowledgeResult resultWithMaxAge(Duration... maxAges) {
        List<KnowledgeFact> facts = new java.util.ArrayList<>();
        for (int i = 0; i < maxAges.length; i++) {
            facts.add(factWithMaxAge("dato " + i, "s" + i, maxAges[i]));
        }
        return new KnowledgeResult(
                facts,
                facts.stream().map(KnowledgeFact::sourceId).toList(),
                List.of(),
                0.9,
                "verificado",
                "KnowledgeEngine",
                "v1");
    }

    private long ttlSeconds(String key) {
        Long ttl = redis.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
        return ttl == null ? -1L : ttl;
    }

    @Test
    void ttlPorFuente_deberiaAplicarElMaxAgeEnElComandoRedis() {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("trm");

        repository.save(query, resultWithMaxAge(Duration.ofHours(12)), Duration.ofDays(365));

        String qk = RedisKnowledgeRepository.KEY_PREFIX
                + RedisKnowledgeRepository.QUERY_KEY
                + Integer.toHexString(
                        (query.topic() + "|" + String.join(",", query.keywords()) + "|" + query.category()).hashCode());
        long qTtl = ttlSeconds(qk);
        assertTrue(
                qTtl >= 43_190 && qTtl <= 43_200,
                "TTL esperado 12h (43200s) por maxAge de la fuente, obtenido " + qTtl);
    }

    @Test
    void ttlMinimo_deberiaPrevalecerConVariasFuentes() {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("mixto");

        repository.save(query, resultWithMaxAge(Duration.ofHours(12), Duration.ofDays(30)), Duration.ofDays(365));

        String qk = RedisKnowledgeRepository.KEY_PREFIX
                + RedisKnowledgeRepository.QUERY_KEY
                + Integer.toHexString(
                        (query.topic() + "|" + String.join(",", query.keywords()) + "|" + query.category()).hashCode());
        long ttl = ttlSeconds(qk);
        assertTrue(ttl >= 43_190 && ttl <= 43_200, "TTL esperado = menor maxAge (12h), obtenido " + ttl);
    }

    @Test
    void ttlSinMaxAge_deberiaUsarElTtlProvisto() {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("general");

        repository.save(query, result("dato", "s1", "https://data.autorizado.com/1"), Duration.ofDays(30));

        String qk = RedisKnowledgeRepository.KEY_PREFIX
                + RedisKnowledgeRepository.QUERY_KEY
                + Integer.toHexString(
                        (query.topic() + "|" + String.join(",", query.keywords()) + "|" + query.category()).hashCode());
        long ttl = ttlSeconds(qk);
        assertTrue(ttl >= 2_591_900 && ttl <= 2_592_000, "TTL esperado = ttl del llamador (30d), obtenido " + ttl);
    }

    @Test
    void ttlCallerMenor_deberiaPrevalecerSobreElMaxAge() {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("corto");

        repository.save(query, resultWithMaxAge(Duration.ofDays(30)), Duration.ofHours(6));

        String qk = RedisKnowledgeRepository.KEY_PREFIX
                + RedisKnowledgeRepository.QUERY_KEY
                + Integer.toHexString(
                        (query.topic() + "|" + String.join(",", query.keywords()) + "|" + query.category()).hashCode());
        long ttl = ttlSeconds(qk);
        assertTrue(ttl >= 21_500 && ttl <= 21_600, "TTL esperado = min(caller 6h, maxAge 30d) = 6h, obtenido " + ttl);
    }

    @Test
    void missLuegoHit_conMismaConsulta() {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("retail", "mercado");

        Optional<KnowledgeResult> miss = repository.find(query);
        assertFalse(miss.isPresent());
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.cache.miss"));

        repository.save(query, result("dato retail", "s1", "https://data.autorizado.com/1"), Duration.ofMinutes(5));

        Optional<KnowledgeResult> hit = repository.find(query);
        assertTrue(hit.isPresent());
        assertEquals("dato retail", hit.get().facts().get(0).claim());
        assertEquals(1.0, metrics.count("kin.knowledge.adapter.cache.hit"));
    }

    @Test
    void ttlExpirado_deberiaVolverAMiss() throws InterruptedException {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("finanzas");

        repository.save(query, result("dato", "s1", "https://data.autorizado.com/1"), Duration.ofMillis(300));

        assertTrue(repository.find(query).isPresent());

        Thread.sleep(600);

        assertFalse(repository.find(query).isPresent());
        assertTrue(metrics.count("kin.knowledge.adapter.cache.miss") >= 1);
    }

    @Test
    void consultasDistintas_noDebenColisionar() {
        RedisKnowledgeRepository repository = repository();
        repository.save(
                query("retail"), result("retail", "s1", "https://data.autorizado.com/1"), Duration.ofMinutes(5));

        Optional<KnowledgeResult> other = repository.find(query("agricultura", "campo"));

        assertFalse(other.isPresent());
    }

    @Test
    void clavesDeterministas_sinDuplicadosAlGuardarDosVeces() {
        RedisKnowledgeRepository repository = repository();
        KnowledgeQuery query = query("retail");
        KnowledgeResult result = result("retail", "s1", "https://data.autorizado.com/1");
        Duration ttl = Duration.ofMinutes(5);

        repository.save(query, result, ttl);
        repository.save(query, result, ttl);

        assertEquals(
                1,
                redis.keys(RedisKnowledgeRepository.KEY_PREFIX + RedisKnowledgeRepository.QUERY_KEY + "*")
                        .size());
        assertTrue(repository.find(query).isPresent());
    }

    @Test
    void soloGuardaDatosPublicos_sinContextoDeUsuario() {
        RedisKnowledgeRepository repository = repository();
        repository.save(
                query("retail"), result("retail", "s1", "https://data.autorizado.com/1"), Duration.ofMinutes(5));

        var keys = redis.keys(RedisKnowledgeRepository.KEY_PREFIX + "*");

        assertFalse(keys.isEmpty());
        for (var key : keys) {
            assertTrue(key.startsWith("kin:knowledge:q:") || key.startsWith("kin:knowledge:c:"));
        }
    }
}
