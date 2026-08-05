package com.kinplatform.ai.knowledge.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de caché distribuida sobre Redis (Fase 13 — infraestructura).
 *
 * <p>Implementa el puerto {@link KnowledgeRepository} del dominio (ADR-014) sin
 * modificar el dominio: almacena únicamente resultados validados, serializados
 * como JSON, con TTL. No conoce el Knowledge Engine ni proveedores.</p>
 *
 * <p><strong>Limitación del contrato congelado</strong>: el puerto
 * {@code save(KnowledgeResult, Duration)} no recibe la {@link KnowledgeQuery}
 * (ADR-014), por lo que {@code find} deriva la clave de la consulta y {@code save}
 * deriva una clave por contenido (hechos ordenados). El cruce hit/miss por turno
 * requiere una ADR aditiva que defina el contrato de clave de caché (ver
 * Fase 6/13). Se activa únicamente con {@code kin.cache.redis.enabled=true}.</p>
 */
public class RedisKnowledgeRepository implements KnowledgeRepository {

    public static final String KEY_PREFIX = "kin:knowledge:";
    public static final String QUERY_KEY = "q:";
    public static final String CONTENT_KEY = "c:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisKnowledgeRepository(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    @Override
    public Optional<KnowledgeResult> find(KnowledgeQuery query) {
        if (query == null || redis == null) {
            return Optional.empty();
        }
        String raw = redis.opsForValue().get(keyForQuery(query));
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(raw, KnowledgeResult.class));
        } catch (JsonProcessingException ex) {
            redis.delete(keyForQuery(query));
            return Optional.empty();
        }
    }

    @Override
    public void save(KnowledgeResult result, Duration ttl) {
        if (result == null || redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(keyForResult(result),
                mapper.writeValueAsString(result), ttl == null ? Duration.ofHours(24) : ttl);
        } catch (JsonProcessingException ignored) {
            // no se cachean resultados no serializables
        }
    }

    private String keyForQuery(KnowledgeQuery query) {
        String seed = query.topic() + "|" + String.join(",", query.keywords());
        return KEY_PREFIX + QUERY_KEY + Integer.toHexString(seed.hashCode());
    }

    private String keyForResult(KnowledgeResult result) {
        String seed = result.facts().stream()
            .map(fact -> fact.sourceId() + "|" + fact.claim())
            .sorted()
            .collect(Collectors.joining("|"));
        return KEY_PREFIX + CONTENT_KEY + Integer.toHexString(seed.hashCode());
    }
}
