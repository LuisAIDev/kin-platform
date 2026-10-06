package com.kinplatform.platform.enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupplementalDataTest {

    @Test
    void calculoCompletoDevuelvePuntoDeEquilibrioCalculado() {
        List<StructuredDatum> data = List.of(
                new StructuredDatum("FINANZAS", "costos_fijos", "100000", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum("FINANZAS", "precio", "10000", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum("FINANZAS", "costos_variables", "5000", StructuredInfoSourceType.USER_INPUT));

        SupplementalData supplemental = SupplementalData.from(data);

        assertThat(supplemental.breakeven().calculable()).isTrue();
        assertThat(supplemental.breakeven().value().value()).isEqualTo("20");
        assertThat(supplemental.breakeven().value().state()).isEqualTo(DataState.CALCULATED);
        assertThat(supplemental.breakeven().value().sourceType()).isEqualTo(StructuredInfoSourceType.CALCULATED);
    }

    @Test
    void calculoIncompletoNoInvenTaYDevuelveFaltantes() {
        List<StructuredDatum> data = List.of(
                new StructuredDatum("FINANZAS", "inversion_inicial", "80000000", StructuredInfoSourceType.USER_INPUT));

        SupplementalData supplemental = SupplementalData.from(data);

        assertThat(supplemental.breakeven().calculable()).isFalse();
        assertThat(supplemental.breakeven().value().state()).isEqualTo(DataState.NOT_AVAILABLE);
        assertThat(supplemental.breakeven().missingFields()).contains("costos_fijos", "precio", "costos_variables");
    }

    @Test
    void datoRealCeroSigueSiendoCero() {
        List<StructuredDatum> data =
                List.of(new StructuredDatum("FINANZAS", "costos_fijos", "0", StructuredInfoSourceType.USER_INPUT));

        SupplementalData supplemental = SupplementalData.from(data);

        ResolvedValue fixedCosts = supplemental.financial("costos_fijos");
        assertThat(fixedCosts.value()).isEqualTo("0");
        assertThat(fixedCosts.state()).isEqualTo(DataState.CONFIRMED);
    }

    @Test
    void datoAusenteEsPendienteNuncaCero() {
        SupplementalData supplemental = SupplementalData.from(List.of());

        ResolvedValue ingresos = supplemental.financial("ingresos");
        assertThat(ingresos.state()).isEqualTo(DataState.PENDING);
        assertThat(ingresos.value()).isNull();
    }

    @Test
    void datoDeDocumentoSePreservaComoImportado() {
        List<StructuredDatum> data = List.of(new StructuredDatum(
                "FINANZAS", "inversion_inicial", "80000000", StructuredInfoSourceType.IMPORTED_DOCUMENT));

        SupplementalData supplemental = SupplementalData.from(data);

        ResolvedValue inversion = supplemental.financial("inversion_inicial");
        assertThat(inversion.sourceType()).isEqualTo(StructuredInfoSourceType.IMPORTED_DOCUMENT);
        assertThat(inversion.state()).isEqualTo(DataState.IMPORTED);
    }
}


