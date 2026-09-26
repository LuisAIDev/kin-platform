package com.kinplatform.kin.health.hce;

import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Preventivo TD-CI-3: este test detecta mismatches SMALLINT/INTEGER antes de prod.
 * No eliminar.
 *
 * <p>Carga el contexto completo con {@code spring.jpa.hibernate.ddl-auto=validate}
 * sobre PostgreSQL real (Testcontainers) con Flyway V1..V76 aplicado. Si alguna
 * columna de las entidades JPA no coincide con el esquema, Hibernate aborta el
 * arranque (igual que en producción) y este test falla ANTES del deploy.</p>
 *
 * <p>Motivo: los tests normales usan {@code ddl-auto=none} (application-test.yml),
 * por lo que no validan el esquema. Este test cierra ese punto ciego (ver TD-CI-3).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class HceSchemaValidationTest extends PostgresTestSupport {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithSchemaValidation() {
        assertThat(context).isNotNull();
    }
}