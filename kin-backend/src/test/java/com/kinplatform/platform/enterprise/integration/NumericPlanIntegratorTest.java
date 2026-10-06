package com.kinplatform.platform.enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.platform.enterprise.valueobjects.MarketPlan;
import com.kinplatform.projectinfo.StructuredInfoSourceType;
import java.util.List;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class NumericPlanIntegratorTest {

    private static final MarketPlan BASE =
            MarketPlan.of(0.0, 0.0, 0.0, 0.0, List.of(), List.of(), List.of(), List.of(), 0.0);

    private static ResolvedValue value(String raw, StructuredInfoSourceType source) {
        return ResolvedValue.of(raw, source, "test");
    }

    @Test
    void entradaVaciaEsIdentidad() {
        MarketPlan result = NumericPlanIntegrator.integrateMarketPlan(BASE, EnterpriseSupplementalInput.empty());

        assertThat(result).isSameAs(BASE);
    }

    @Test
    void enriqueceTamSamSomYGrowthRate() {
        EnterpriseSupplementalInput input = new EnterpriseSupplementalInput(
                value("4500000000", StructuredInfoSourceType.IMPORTED_DOCUMENT),
                value("1800000000", StructuredInfoSourceType.USER_INPUT),
                value("900000000", StructuredInfoSourceType.USER_INPUT),
                value("12", StructuredInfoSourceType.ESTIMATED),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending());

        MarketPlan result = NumericPlanIntegrator.integrateMarketPlan(BASE, input);

        assertThat(result.tam()).isEqualTo(4500000000.0);
        assertThat(result.sam()).isEqualTo(1800000000.0);
        assertThat(result.som()).isEqualTo(900000000.0);
        assertThat(result.growthRate()).isEqualTo(12.0);
    }

    @Test
    void soloSomSeEnriqueceYElRestoSeConserva() {
        MarketPlan base = MarketPlan.of(100.0, 50.0, 10.0, 5.0, List.of(), List.of(), List.of(), List.of(), 0.5);
        EnterpriseSupplementalInput input = new EnterpriseSupplementalInput(
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                value("900000000", StructuredInfoSourceType.USER_INPUT),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending());

        MarketPlan result = NumericPlanIntegrator.integrateMarketPlan(base, input);

        assertThat(result.som()).isEqualTo(900000000.0);
        assertThat(result.tam()).isEqualTo(100.0);
        assertThat(result.growthRate()).isEqualTo(5.0);
        assertThat(result.confidence()).isEqualTo(0.5);
    }

    @Test
    void valorNegativoNoSeUtiliza() {
        EnterpriseSupplementalInput input = new EnterpriseSupplementalInput(
                value("-5", StructuredInfoSourceType.USER_INPUT),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending());

        MarketPlan result = NumericPlanIntegrator.integrateMarketPlan(BASE, input);

        assertThat(result).isSameAs(BASE);
    }

    @Test
    void ingresosSeCalculaConPrecioYUnidades() {
        OptionalDouble ingresos = NumericPlanIntegrator.ingresos(
                value("10000", StructuredInfoSourceType.USER_INPUT),
                value("5000", StructuredInfoSourceType.USER_INPUT));

        assertThat(ingresos).isPresent();
        assertThat(ingresos.getAsDouble()).isEqualTo(50_000_000.0);
    }

    @Test
    void ingresosSinDatosEsVacio() {
        assertThat(NumericPlanIntegrator.ingresos(
                        ResolvedValue.pending(), value("5", StructuredInfoSourceType.USER_INPUT)))
                .isEmpty();
        assertThat(NumericPlanIntegrator.ingresos(
                        value("5", StructuredInfoSourceType.USER_INPUT), ResolvedValue.pending()))
                .isEmpty();
    }

    @Test
    void margenDeContribucionSeCalcula() {
        OptionalDouble margin = NumericPlanIntegrator.contributionMargin(
                value("10000", StructuredInfoSourceType.USER_INPUT),
                value("4000", StructuredInfoSourceType.USER_INPUT));

        assertThat(margin).isPresent();
        assertThat(margin.getAsDouble()).isCloseTo(0.6, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void puntoDeEquilibrioSeCalculaConTodosLosDatos() {
        OptionalDouble breakEven = NumericPlanIntegrator.breakEvenUnits(
                value("120000000", StructuredInfoSourceType.IMPORTED_DOCUMENT),
                value("10000", StructuredInfoSourceType.USER_INPUT),
                value("4000", StructuredInfoSourceType.USER_INPUT));

        assertThat(breakEven).isPresent();
        assertThat(breakEven.getAsDouble()).isCloseTo(20_000.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void puntoDeEquilibrioIncompletoEsVacio() {
        assertThat(NumericPlanIntegrator.breakEvenUnits(
                        value("120000000", StructuredInfoSourceType.USER_INPUT),
                        value("10000", StructuredInfoSourceType.USER_INPUT),
                        ResolvedValue.pending()))
                .isEmpty();
    }

    @Test
    void puntoDeEquilibrioConMargenNoPositivoEsVacio() {
        assertThat(NumericPlanIntegrator.breakEvenUnits(
                        value("120000000", StructuredInfoSourceType.USER_INPUT),
                        value("4000", StructuredInfoSourceType.USER_INPUT),
                        value("6000", StructuredInfoSourceType.USER_INPUT)))
                .isEmpty();
    }
}


