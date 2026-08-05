package com.kinplatform.ai.knowledge.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Cableado opcional del adaptador Redis (Fase 13 — infraestructura).
 *
 * <p>Solo se activa con {@code kin.cache.redis.enabled=true}; por defecto está
 * deshabilitado, por lo que el comportamiento observable de dev/test/prod no
 * cambia. No modifica el dominio ni ADR-014/012.</p>
 */
@Configuration
@ConditionalOnProperty(name = "kin.cache.redis.enabled", havingValue = "true")
public class RedisCacheConfig {

    @Bean
    public RedisKnowledgeRepository redisKnowledgeRepository(StringRedisTemplate redis,
                                                             ObjectMapper mapper) {
        return new RedisKnowledgeRepository(redis, mapper);
    }
}
