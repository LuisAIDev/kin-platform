package com.kinplatform;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Carga el contexto COMPLETO de Spring Boot (perfil {@code test}, PostgreSQL
 * real vía Testcontainers). Su objetivo es detectar beans faltantes que los
 * tests con mocks no ven: p. ej. el {@code WebClient} requerido por los
 * adapters de MIPRES, cuyo faltante tumbó el arranque en producción.
 *
 * <p>Los beans marcados {@code @Profile("prod")} no se instancian aquí (el
 * perfil activo es {@code test}); por eso además verificamos explícitamente que
 * el bean {@link WebClient} exista, que es el contrato del que dependen
 * {@code MipresHttpClient} y {@code MipresTokenService}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest extends PostgresTestSupport {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void webClientBeanIsWired() {
        assertThat(context.getBean(WebClient.class)).isNotNull();
    }
}
