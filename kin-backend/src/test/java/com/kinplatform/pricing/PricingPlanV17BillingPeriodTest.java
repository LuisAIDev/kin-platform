package com.kinplatform.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Reproduce el fallo de producción: bases históricas donde
 * {@code pricing_plans.billing_period} (y {@code currency}) son {@code NOT
 * NULL} SIN {@code DEFAULT} (creadas por un init.sql previo a Flyway). La
 * corrección de V17 garantiza el default {@code 'monthly'/'USD'} y fija los
 * valores en el INSERT de STANDARD, de modo que la migración y los INSERTs de
 * Hibernate (que no mapean estas columnas) nunca introducen NULL.
 */
class PricingPlanV17BillingPeriodTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("kin_test")
            .withUsername("kin_test")
            .withPassword("kin_test");

    static {
        PG.start();
    }

    private void exec(Statement st, String sql) throws Exception {
        st.execute(sql);
    }

    @Test
    void v17_compatibleConBasesHistoricasSinDefault() throws Exception {
        try (Connection c = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement st = c.createStatement()) {

            // Esquema histórico: billing_period/currency/display_order/is_popular
            // NOT NULL SIN DEFAULT (causa del fallo de producción en V17).
            exec(
                    st,
                    "CREATE TABLE pricing_plans ("
                            + "id UUID PRIMARY KEY DEFAULT gen_random_uuid(), "
                            + "name VARCHAR(100) NOT NULL, price NUMERIC(10,2) NOT NULL, "
                            + "currency VARCHAR(3) NOT NULL, billing_period VARCHAR(20) NOT NULL, "
                            + "features JSONB NOT NULL, is_popular BOOLEAN NOT NULL, "
                            + "display_order INTEGER NOT NULL, "
                            + "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), "
                            + "description TEXT, max_projects INTEGER, messages_per_month INTEGER, "
                            + "advanced_ai BOOLEAN NOT NULL DEFAULT FALSE, pdf_export BOOLEAN NOT NULL DEFAULT FALSE, "
                            + "support_level VARCHAR(20) NOT NULL DEFAULT 'BASIC', "
                            + "viability_scoring_detail VARCHAR(20) NOT NULL DEFAULT 'BASIC', "
                            + "is_active BOOLEAN NOT NULL DEFAULT TRUE)");

            // Planes legacy sin code (display_order NOT NULL sin default -> obligatorio).
            exec(
                    st,
                    "INSERT INTO pricing_plans (id, name, price, currency, billing_period, features, is_popular, display_order) VALUES "
                            + "(gen_random_uuid(), 'Básico Gratis', 0.00, 'USD', 'monthly', '[]'::jsonb, FALSE, 1), "
                            + "(gen_random_uuid(), 'Premium Pro', 10.00, 'USD', 'monthly', '[]'::jsonb, TRUE, 2)");

            // Sentencias de V17 (corregidas).
            exec(st, "ALTER TABLE pricing_plans ADD COLUMN code VARCHAR(20)");
            exec(st, "ALTER TABLE pricing_plans ADD COLUMN ai_budget_usd NUMERIC(10,2)");
            exec(st, "CREATE UNIQUE INDEX IF NOT EXISTS uq_pricing_plans_code ON pricing_plans (code)");
            exec(st, "ALTER TABLE pricing_plans ALTER COLUMN billing_period SET DEFAULT 'monthly'");
            exec(st, "ALTER TABLE pricing_plans ALTER COLUMN currency SET DEFAULT 'USD'");
            exec(st, "ALTER TABLE pricing_plans ALTER COLUMN display_order SET DEFAULT 0");
            exec(st, "ALTER TABLE pricing_plans ALTER COLUMN is_popular SET DEFAULT FALSE");
            exec(
                    st,
                    "UPDATE pricing_plans SET code='FREE', ai_budget_usd=0.50 "
                            + "WHERE name='Básico Gratis' AND code IS NULL");
            exec(
                    st,
                    "UPDATE pricing_plans SET code='PREMIUM', ai_budget_usd=8.75, max_projects=NULL, display_order=3 "
                            + "WHERE name='Premium Pro' AND code IS NULL");
            exec(
                    st,
                    "INSERT INTO pricing_plans (id, name, description, price, currency, billing_period, features, "
                            + "display_order, is_popular, max_projects, messages_per_month, advanced_ai, pdf_export, "
                            + "support_level, viability_scoring_detail, is_active, code, ai_budget_usd, created_at, updated_at) "
                            + "SELECT gen_random_uuid(), 'STANDARD', 'Plan ideal', 25.00, 'USD', 'monthly', "
                            + "'[]'::json, 2, FALSE, 5, 500, TRUE, TRUE, 'PREMIUM', 'DETAILED', TRUE, 'STANDARD', 6.25, "
                            + "now(), now() WHERE NOT EXISTS (SELECT 1 FROM pricing_plans WHERE code='STANDARD')");

            // STANDARD: todos los campos de la corrección.
            try (ResultSet rs = st.executeQuery(
                    "SELECT code, price, max_projects, ai_budget_usd, billing_period, currency, display_order, is_popular "
                            + "FROM pricing_plans WHERE code='STANDARD'")) {
                assertTrue(rs.next());
                assertEquals("STANDARD", rs.getString("code"));
                assertEquals(0, rs.getBigDecimal("price").compareTo(new BigDecimal("25.00")));
                assertEquals(5, rs.getInt("max_projects"));
                assertEquals(0, rs.getBigDecimal("ai_budget_usd").compareTo(new BigDecimal("6.25")));
                assertEquals("monthly", rs.getString("billing_period"));
                assertEquals("USD", rs.getString("currency"));
                assertEquals(2, rs.getInt("display_order"));
                assertEquals(false, rs.getBoolean("is_popular"));
            }

            // FREE/PREMIUM preservan columnas obligatorias y re-secuencian display_order.
            try (ResultSet rs =
                    st.executeQuery("SELECT code, billing_period, currency, display_order FROM pricing_plans "
                            + "WHERE code IN ('FREE','PREMIUM') ORDER BY code")) {
                assertTrue(rs.next());
                assertEquals("FREE", rs.getString("code"));
                assertEquals("monthly", rs.getString("billing_period"));
                assertEquals("USD", rs.getString("currency"));
                assertEquals(1, rs.getInt("display_order"));
                assertTrue(rs.next());
                assertEquals("PREMIUM", rs.getString("code"));
                assertEquals("monthly", rs.getString("billing_period"));
                assertEquals("USD", rs.getString("currency"));
                assertEquals(3, rs.getInt("display_order"));
            }

            // Después de SET DEFAULT, un INSERT de Hibernate (sin billing_period,
            // display_order ni is_popular) obtiene los defaults — nunca NULL.
            exec(
                    st,
                    "INSERT INTO pricing_plans (id, name, price, features, code) "
                            + "VALUES (gen_random_uuid(), 'Plan X', 1.00, '[]'::jsonb, 'X')");
            try (ResultSet rs = st.executeQuery(
                    "SELECT billing_period, display_order, is_popular FROM pricing_plans WHERE code='X'")) {
                assertTrue(rs.next());
                assertEquals("monthly", rs.getString("billing_period"));
                assertEquals(0, rs.getInt("display_order"));
                assertEquals(false, rs.getBoolean("is_popular"));
            }

            // code único: un segundo 'STANDARD' viola el índice UNIQUE.
            assertThrows(
                    java.sql.SQLException.class,
                    () -> exec(
                            st,
                            "INSERT INTO pricing_plans (id, name, price, features, code) "
                                    + "VALUES (gen_random_uuid(), 'Otro', 1.00, '[]'::jsonb, 'STANDARD')"));
        }
    }
}
