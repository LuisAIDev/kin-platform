package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.reporting.report.model.DimensionCoverage;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InnovationSectionFormatterTest {

    private final InnovationSectionFormatter formatter = new InnovationSectionFormatter();

    @Test
    void kind_deberiaSerProjection() {
        assertEquals(ReportSectionKind.PROJECTION, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosDatosConSenalesYCobertura() {
        var coverage = List.of(
            new DimensionCoverage(AnalyzedDimension.VALUE_PROPOSITION, true),
            new DimensionCoverage(AnalyzedDimension.MVP, true)
        );
        var section = new InnovationSection("App de delivery saludable", "Comida sana a domicilio",
            "MVP con 10 restaurantes", List.of("Modelo de suscripción", "IA de recomendación"), coverage);

        var result = formatter.format(section);

        assertTrue(result.contains("## Innovación"));
        assertTrue(result.contains("**Solución:** App de delivery saludable"));
        assertTrue(result.contains("**Propuesta de valor:** Comida sana a domicilio"));
        assertTrue(result.contains("**MVP:** MVP con 10 restaurantes"));
        assertTrue(result.contains("### Señales de Innovación"));
        assertTrue(result.contains("- Modelo de suscripción"));
        assertTrue(result.contains("- IA de recomendación"));
        assertTrue(result.contains("### Cobertura de Dimensión"));
        assertTrue(result.contains("- **Propuesta de valor:** Cubierto"));
        assertTrue(result.contains("- **MVP / validación temprana:** Cubierto"));
    }

    @Test
    void format_noDeberiaIncluirSenales_cuandoEstanVacias() {
        var section = new InnovationSection("Solución", "", "", List.of(), List.of());

        var result = formatter.format(section);

        assertTrue(result.contains("**Solución:** Solución"));
        assertFalse(result.contains("### Señales de Innovación"));
        assertFalse(result.contains("**Propuesta de valor:**"));
        assertFalse(result.contains("**MVP:**"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoSeccionVacia() {
        var result = formatter.format(InnovationSection.empty());

        assertTrue(result.contains("## Innovación"));
        assertTrue(result.contains("_Sin datos de innovación disponibles._"));
    }
}
