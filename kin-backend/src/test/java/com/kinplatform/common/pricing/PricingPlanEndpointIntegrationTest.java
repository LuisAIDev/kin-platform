package com.kinplatform.common.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.test.PostgresTestSupport;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regresión del 500 reportado en {@code /dashboard/plans}. Ejercita el endpoint
 * real (controller + servicio + PostgreSQL + Flyway) y verifica que devuelve
 * 200 con exactamente los tres planes canónicos de EMPRESAS, cada uno con su
 * precio en USD ({@code price}) y en COP ({@code priceCop}).
 *
 * <p>También garantiza que V86 deja fuera del catálogo los planes legacy sin
 * {@code code} que provocaban tarjetas duplicadas y sin precio en COP.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PricingPlanEndpointIntegrationTest extends PostgresTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void getByVertical_empresas_devuelve200ConPreciosUsdYCop() throws Exception {
        var response = mockMvc.perform(get("/pricing-plans/vertical/EMPRESAS"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        JsonNode plans = MAPPER.readTree(response.getContentAsByteArray());

        assertThat(plans).hasSize(3);
        assertPlan(plans, "FREE", "0.00", "0.00");
        assertPlan(plans, "STANDARD", "15.00", "60000.00");
        assertPlan(plans, "PREMIUM", "22.00", "88000.00");
    }

    @Test
    void v86_ocultaPlanesLegacySinCodeYDejaSoloLosCanonicos() throws Exception {
        jdbcTemplate.update(
                """
                INSERT INTO pricing_plans
                    (id, name, description, price, features, is_active, vertical, code,
                     support_level, viability_scoring_detail, advanced_ai, pdf_export,
                     max_projects, messages_per_month, created_at, updated_at)
                VALUES
                    (gen_random_uuid(), 'GRATIS', 'Plan legacy', 0.00, '[]'::jsonb, TRUE,
                     'EMPRESAS', NULL, 'BASIC', 'BASIC', FALSE, FALSE, 3, 100, NOW(), NOW())
                """);

        applyMigration("db/migration/V86__consolidate_legacy_empresas_plans.sql");

        var response = mockMvc.perform(get("/pricing-plans/vertical/EMPRESAS"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        JsonNode plans = MAPPER.readTree(response.getContentAsByteArray());

        assertThat(plans).hasSize(3);
        assertPlan(plans, "FREE", "0.00", "0.00");
        assertPlan(plans, "STANDARD", "15.00", "60000.00");
        assertPlan(plans, "PREMIUM", "22.00", "88000.00");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM pricing_plans "
                                + "WHERE vertical = 'EMPRESAS' AND code IS NULL AND is_active = TRUE",
                        Integer.class))
                .isZero();
    }

    private void applyMigration(String classpath) throws Exception {
        var sql = new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var withoutComments = Arrays.stream(sql.split("\\R"))
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (acc, line) -> acc + line + "\n");

        for (String statement : withoutComments.split(";")) {
            if (!statement.isBlank()) {
                jdbcTemplate.execute(statement);
            }
        }
    }

    private void assertPlan(JsonNode plans, String code, String usd, String cop) {
        JsonNode plan = null;
        for (JsonNode candidate : plans) {
            if (code.equals(candidate.path("code").asText(null))) {
                plan = candidate;
                break;
            }
        }

        assertThat(plan)
                .as("Plan EMPRESAS %s presente y sin duplicados legacy", code)
                .isNotNull();
        assertThat(plan.path("price").decimalValue()).isEqualByComparingTo(new BigDecimal(usd));
        assertThat(plan.path("priceCop").decimalValue()).isEqualByComparingTo(new BigDecimal(cop));
    }
}
