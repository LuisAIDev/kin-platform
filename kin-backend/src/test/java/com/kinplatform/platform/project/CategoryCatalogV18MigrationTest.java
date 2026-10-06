package com.kinplatform.platform.project;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Validación documental de V18 (sin ejecutar la migración): confirma que el
 * catálogo de categorías se amplía (GASTRONOMIA renombrada, SERVICIOS y OTRO
 * añadidos) de forma idempotente y sin tocar UUIDs existentes ni eliminar nada.
 */
class CategoryCatalogV18MigrationTest {

    private static String sql;

    @BeforeAll
    static void loadMigration() throws IOException {
        var resource = new ClassPathResource("db/migration/V18__add_project_categories.sql");
        sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void renombraGastronomiaParaCubrirFoodAndBeverage() {
        assertTrue(sql.contains("UPDATE categories"));
        assertTrue(sql.contains("SET name = 'Gastronom\u00eda y Alimentos'"));
        assertTrue(sql.contains("WHERE code = 'GASTRONOMIA'"));
    }

    @Test
    void anadeServiciosYOtraCategoria() {
        assertTrue(sql.contains("'SERVICIOS', 'Servicios'"));
        assertTrue(sql.contains("'OTRO', 'Otro / Sin clasificar'"));
        assertTrue(sql.contains("display_order"));
    }

    @Test
    void esIdempotente() {
        assertTrue(sql.contains("ON CONFLICT DO NOTHING"));
    }

    @Test
    void noEliminaNiAlteraUuidsExistentes() {
        assertFalse(sql.contains("DROP"));
        assertFalse(sql.contains("DELETE"));
        assertTrue(sql.contains("11111111-1111-1111-1111-111111111112"));
        assertTrue(sql.contains("11111111-1111-1111-1111-111111111113"));
    }
}

