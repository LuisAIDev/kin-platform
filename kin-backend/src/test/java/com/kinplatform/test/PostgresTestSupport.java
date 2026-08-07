package com.kinplatform.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Soporte base para tests que requieren PostgreSQL 18 real proporcionado por
 * Testcontainers (paridad con Neon, AD-8). El contenedor es {@code static} y
 * compartido por todas las clases que extienden esta base: se arranca una sola
 * vez por JVM y cada contexto de Spring ejecuta Flyway V1..V11 sobre él. La
 * suite crea y destruye su propia base temporal; nunca apunta a Neon.
 *
 * <p>Los valores del datasource se inyectan con {@code @DynamicPropertySource}
 * (precedencia máxima), de modo que ni {@code application-test.yml} ni el
 * {@code .env} interfieren.</p>
 */
public abstract class PostgresTestSupport {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("kin_test")
            .withUsername("kin_test")
            .withPassword("kin_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
