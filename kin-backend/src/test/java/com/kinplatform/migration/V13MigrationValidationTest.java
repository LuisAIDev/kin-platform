package com.kinplatform.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Validación documental de V13 (sin ejecutar la migración): confirma que la
 * migración contiene los elementos de seguridad requeridos.
 */
class V13MigrationValidationTest {

    private static String sql;

    @BeforeAll
    static void loadMigration() throws IOException {
        var resource = new ClassPathResource("db/migration/V13__add_email_verification.sql");
        sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void agregaEmailVerifiedPorDefectoFalso() {
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE"));
    }

    @Test
    void backfillMarcaUsuariosExistentesComoVerificados() {
        assertTrue(sql.contains("UPDATE users SET email_verified = TRUE"));
    }

    @Test
    void tablaDeTokensTieneLosCamposRequeridos() {
        assertTrue(sql.contains("CREATE TABLE email_verification_tokens"));
        assertTrue(sql.contains("token_hash  VARCHAR(64) NOT NULL"));
        assertTrue(sql.contains("expires_at  TIMESTAMPTZ NOT NULL"));
        assertTrue(sql.contains("used_at     TIMESTAMPTZ"));
        assertTrue(sql.contains("created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()"));
    }

    @Test
    void tokenHashEsUnico() {
        assertTrue(sql.contains("CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash)"));
    }

    @Test
    void existeFkConOnDeleteCascade() {
        assertTrue(sql.contains("FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE"));
    }

    @Test
    void existenIndicesEsperados() {
        assertTrue(sql.contains("CREATE INDEX idx_email_verification_tokens_user"));
        assertTrue(sql.contains("CREATE INDEX idx_email_verification_tokens_expires"));
    }
}
