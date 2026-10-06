package com.kinplatform.platform.enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnterpriseSupplementalInputTest {

    @Test
    void desdeSupplementalDataMapeaCamposTipadosConOrigen() {
        SupplementalData supplemental = SupplementalData.from(List.of(
                new StructuredDatum("MERCADO", "tam", "4500000000", StructuredInfoSourceType.IMPORTED_DOCUMENT),
                new StructuredDatum("MERCADO", "som", "900000000", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum("MERCADO", "growth_rate", "12", StructuredInfoSourceType.ESTIMATED),
                new StructuredDatum("FINANZAS", "precio", "10000", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum("FINANZAS", "ventas_estimadas", "5000", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum("FINANZAS", "costos_variables", "4000", StructuredInfoSourceType.USER_INPUT)));

        EnterpriseSupplementalInput input = EnterpriseSupplementalInput.from(supplemental);

        assertThat(input.tam().value()).isEqualTo("4500000000");
        assertThat(input.tam().sourceType()).isEqualTo(StructuredInfoSourceType.IMPORTED_DOCUMENT);
        assertThat(input.tam().state()).isEqualTo(DataState.IMPORTED);
        assertThat(input.som().value()).isEqualTo("900000000");
        assertThat(input.som().state()).isEqualTo(DataState.CONFIRMED);
        assertThat(input.growthRate().value()).isEqualTo("12");
        assertThat(input.growthRate().state()).isEqualTo(DataState.ESTIMATED);
        assertThat(input.unitPrice().value()).isEqualTo("10000");
        assertThat(input.units().value()).isEqualTo("5000");
        assertThat(input.variableCost().value()).isEqualTo("4000");
    }

    @Test
    void datoNoNumericoSeMarcaComoNoDisponible() {
        SupplementalData supplemental = SupplementalData.from(
                List.of(new StructuredDatum("FINANZAS", "precio", "alto", StructuredInfoSourceType.USER_INPUT)));

        EnterpriseSupplementalInput input = EnterpriseSupplementalInput.from(supplemental);

        assertThat(input.unitPrice().state()).isEqualTo(DataState.NOT_AVAILABLE);
        assertThat(input.unitPrice().value()).isNull();
    }

    @Test
    void sinDatosLaEntradaEstaVacia() {
        EnterpriseSupplementalInput input = EnterpriseSupplementalInput.empty();

        assertThat(input.isEmpty()).isTrue();
    }

    @Test
    void aliasDeCrecimientoYUnidadesSeResuelven() {
        SupplementalData supplemental = SupplementalData.from(List.of(
                new StructuredDatum("MERCADO", "crecimiento", "8", StructuredInfoSourceType.ESTIMATED),
                new StructuredDatum("FINANZAS", "unidades", "12000", StructuredInfoSourceType.USER_INPUT)));

        EnterpriseSupplementalInput input = EnterpriseSupplementalInput.from(supplemental);

        assertThat(input.growthRate().value()).isEqualTo("8");
        assertThat(input.units().value()).isEqualTo("12000");
    }
}


