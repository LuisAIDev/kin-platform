package com.kinplatform.ai.knowledge.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Adaptador de caché distribuida sobre Redis (Fase 13 — infraestructura,
 * contrato de clave definido por ADR-021).
 *
 * <p>Implementa el puerto {@link KnowledgeRepository} del dominio (ADR-014) sin
 * modificar el dominio: almacena únicamente resultados validados, serializados
 * como JSON, con TTL. No conoce el Knowledge Engine ni proveedores.</p>
 *
 * <p><strong>Claves deterministas (ADR-021):</strong> la sobrecarga aditiva
 * {@link #save(KnowledgeQuery, KnowledgeResult, Duration)} escribe la clave
 * derivada de la consulta ({@code kin:knowledge:q:<hash>}) — la misma que usa
 * {@link #find(KnowledgeQuery)} — y una clave secundaria por contenido
 * ({@code kin:knowledge:c:<hash>}) para deduplicación. La implementación por
 * defecto {@link #save(KnowledgeResult, Duration)} conserva la semántica
 * congelada (solo clave de contenido). El prefijo {@code kin:knowledge:} aísla
 * el dominio de caché; solo se guardan hechos públicos validados (nunca PII ni
 * contexto de usuario). Se activa únicamente con
 * {@code kin.cache.redis.enabled=true}.</p>
 */
public class RedisKnowledgeRepository implements KnowledgeRepository {

    public static final String KEY_PREFIX = "kin:knowledge:";
    public static final String QUERY_KEY = "q:";
    public static final String CONTENT_KEY = "c:";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final KnowledgeAdapterMetrics metrics;

    public RedisKnowledgeRepository(StringRedisTemplate redis, ObjectMapper mapper) {
        this(redis, mapper, null);
    }

    public RedisKnowledgeRepository(StringRedisTemplate redis, ObjectMapper mapper, KnowledgeAdapterMetrics metrics) {
        this.redis = redis;
        this.mapper = ensureJavaTime(mapper == null ? new ObjectMapper() : mapper);
        this.metrics = metrics == null ? new KnowledgeAdapterMetrics(null) : metrics;
    }

    /**
     * Garantiza el soporte de {@code OffsetDateTime} en el mapper usado para
     * serializar {@link KnowledgeResult} (los hechos llevan {@code publishedAt})
     * y una deserialización tolerante: los records de dominio exponen helpers de
     * solo lectura ({@code isEmpty()}, {@code factCount()}) que Jackson emite
     * como propiedades JSON extra; al releerlas no son componentes del record y
     * deben ignorarse en vez de fallar. Si el mapper inyectado (Spring) ya lo
     * tiene, copiarlo y re-registrar es idempotente.
     */
    private static ObjectMapper ensureJavaTime(ObjectMapper source) {
        ObjectMapper copy = source.copy();
        copy.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        copy.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        copy.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return copy;
    }

    @Override
    public Optional<KnowledgeResult> find(KnowledgeQuery query) {
        if (query == null || redis == null) {
            return Optional.empty();
        }
        String raw = redis.opsForValue().get(keyForQuery(query));
        if (raw == null) {
            metrics.cacheMiss();
            return Optional.empty();
        }
        try {
            Optional<KnowledgeResult> result = Optional.of(mapper.readValue(raw, KnowledgeResult.class));
            if (result.isPresent() && !result.get().isEmpty()) {
                metrics.cacheHit();
            } else {
                redis.delete(keyForQuery(query));
                metrics.cacheMiss();
                return Optional.empty();
            }
            return result;
        } catch (JsonProcessingException ex) {
            redis.delete(keyForQuery(query));
            metrics.cacheMiss();
            return Optional.empty();
        }
    }

    @Override
    public void save(KnowledgeResult result, Duration ttl) {
        if (result == null || redis == null) {
            return;
        }
        saveByKey(keyForResult(result), result, ttl);
    }

    @Override
    public void save(KnowledgeQuery query, KnowledgeResult result, Duration ttl) {
        if (query == null || result == null || redis == null) {
            return;
        }
        String json = serialize(result);
        if (json == null) {
            return;
        }
        Duration effective = effectiveTtl(result, ttl);
        redis.opsForValue().set(keyForQuery(query), json, effective);
        redis.opsForValue().set(keyForResult(result), json, effective);
    }

    private void saveByKey(String key, KnowledgeResult result, Duration ttl) {
        if (result == null || key == null || redis == null) {
            return;
        }
        String json = serialize(result);
        if (json == null) {
            return;
        }
        redis.opsForValue().set(key, json, effectiveTtl(result, ttl));
    }

    /**
     * TTL dinámico e independiente por fuente (ADR-025): prevalece la frescura.
     * Si los hechos del resultado declaran {@code maxAge}, el TTL efectivo es el
     * mínimo entre el {@code ttl} del llamador y el mínimo de esos {@code maxAge}
     * (la fuente que cambia más seguido fija la expiración). Si ningún hecho
     * declara {@code maxAge}, se usa el {@code ttl} del llamador (o 24 h si es
     * {@code null}) — retrocompatibilidad con el contrato congelado de ADR-014.
     */
    private Duration effectiveTtl(KnowledgeResult result, Duration ttl) {
        Duration base = ttl == null ? Duration.ofHours(24) : ttl;
        return result.effectiveTtl()
                .map(fresh -> base.compareTo(fresh) <= 0 ? base : fresh)
                .orElse(base);
    }

    private String serialize(KnowledgeResult result) {
        try {
            return mapper.writeValueAsString(result);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private String keyForQuery(KnowledgeQuery query) {
        return queryKey(query);
    }

    private String keyForResult(KnowledgeResult result) {
        return contentKey(result);
    }

    /**
     * Clave determinista por consulta (ADR-021). La categoría del proyecto
     * (ADR-024) se incluye para aislar el caché cuando la selección de fuentes
     * depende de ella. Package-private para tests de determinismo y aislamiento.
     */
    static String queryKey(KnowledgeQuery query) {
        String seed = query.topic() + "|" + String.join(",", query.keywords()) + "|" + query.category();
        return KEY_PREFIX + QUERY_KEY + Integer.toHexString(seed.hashCode());
    }

    /**
     * Clave determinista por contenido (deduplicación, ADR-021). Package-private
     * para tests.
     */
    static String contentKey(KnowledgeResult result) {
        String seed = result.facts().stream()
                .map(fact -> fact.sourceId() + "|" + fact.claim())
                .sorted()
                .collect(Collectors.joining("|"));
        return KEY_PREFIX + CONTENT_KEY + Integer.toHexString(seed.hashCode());
    }
}
