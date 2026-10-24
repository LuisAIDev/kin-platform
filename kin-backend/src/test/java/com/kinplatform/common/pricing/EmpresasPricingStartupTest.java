package com.kinplatform.common.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.test.PostgresTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Verifica que los precios aprobados de EMPRESAS sobrevivan al arranque:
 * Flyway (V85) fija los valores y {@link DataInitializer} ya no los sobrescribe.
 * También cubre la instalación nueva, donde FREE/PREMIUM los crea el
 * inicializador con su {@code price_cop}.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class EmpresasPricingStartupTest extends PostgresTestSupport {

    @Autowired
    private PricingPlanRepository repository;

    @Test
    void preciosEmpresasSobrevivenAlArranque() {
        assertPrecio("FREE", "0.00", "0.00");
        assertPrecio("STANDARD", "15.00", "60000.00");
        assertPrecio("PREMIUM", "22.00", "88000.00");
    }

    private void assertPrecio(String code, String usd, String cop) {
        PricingPlan plan = repository
                .findByCodeAndVertical(code, ProductVertical.EMPRESAS)
                .orElseThrow(() -> new AssertionError("Plan EMPRESAS no encontrado: " + code));

        assertThat(plan.getPrice()).isEqualByComparingTo(new BigDecimal(usd));
        assertThat(plan.getPriceCop()).isEqualByComparingTo(new BigDecimal(cop));
    }
}
