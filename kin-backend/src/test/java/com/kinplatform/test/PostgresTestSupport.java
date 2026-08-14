package com.kinplatform.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Soporte base para tests que requieren PostgreSQL 18 real proporcionado por
 * Testcontainers (paridad con Neon, AD-8). El contenedor es {@code static} y
 * compartido por todas las clases que extienden esta base: se arranca una sola
 * vez por JVM y cada contexto de Spring ejecuta Flyway V1..V15 sobre él. La
 * suite crea y destruye su propia base temporal; nunca apunta a Neon.
 *
 * <p>Los valores del datasource se inyectan con {@code @DynamicPropertySource}
 * (precedencia máxima), de modo que ni {@code application-test.yml} ni el
 * {@code .env} interfieran.</p>
 *
 * <p><b>Aislamiento entre clases:</b> como el contenedor es compartido, se
 * limpia el esquema {@code public} (excepto {@code flyway_schema_history})
 * antes de cada clase de tests con {@link #cleanDatabaseBeforeClass()}, para
 * que las clases no compartan datos residuales (p. ej. planes sembrados por el
 * {@code DataInitializer} de un contexto {@code @SpringBootTest} completo o
 * datos de un {@code @BeforeEach} de otra clase).</p>
 */
public abstract class PostgresTestSupport {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("kin_test")
            .withUsername("kin_test")
            .withPassword("kin_test");

    static {
        POSTGRES.start();
    }

    @BeforeAll
    static void cleanDatabaseBeforeClass() {
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$
                    DECLARE r RECORD;
                    BEGIN
                        FOR r IN SELECT tablename
                                   FROM pg_tables
                                  WHERE schemaname = 'public'
                                    AND tablename <> 'flyway_schema_history'
                        LOOP
                            EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' CASCADE';
                        END LOOP;
                    END $$;
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo limpiar la base de tests antes de la clase (Testcontainers)", e);
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
