package com.kinplatform.institutional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que String -> jsonb persiste correctamente (regresion del binding
 * VARCHAR vs jsonb que causaba HTTP 500 en POST /institutional/branches).
 */
@SpringBootTest
@ActiveProfiles("test")
class BranchIntegrationTest extends PostgresTestSupport {

    @Autowired
    private BranchRepository repository;

    @Test
    void createBranch_withServicesEnabled_persistsAsJsonb() throws Exception {
        String json = "{\"urgencias\":true,\"consulta\":true}";
        Branch saved = repository.saveAndFlush(Branch.builder()
                .organizationId(UUID.randomUUID())
                .name("Sede JSON " + UUID.randomUUID())
                .servicesEnabled(json)
                .build());

        Branch found = repository.findById(saved.getId()).orElseThrow();
        var mapper = new ObjectMapper();
        assertEquals(mapper.readTree(json), mapper.readTree(found.getServicesEnabled()),
                "jsonb debe preservar el contenido semantico");
        assertTrue(found.getServicesEnabled().contains("urgencias"));
    }

    @Test
    void createBranch_withNullServicesEnabled_persists() {
        Branch saved = repository.saveAndFlush(Branch.builder()
                .organizationId(UUID.randomUUID())
                .name("Sede Null " + UUID.randomUUID())
                .servicesEnabled(null)
                .build());

        assertNull(repository.findById(saved.getId()).orElseThrow().getServicesEnabled());
    }
}
