package com.kinplatform.kin.ai.prompt.formatter;

import com.kinplatform.kin.reporting.report.model.NextStep;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.ReportSectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NextStepsSectionFormatterTest {

    private final NextStepsSectionFormatter formatter = new NextStepsSectionFormatter();

    @Test
    void kind_deberiaSerAggregate() {
        assertEquals(ReportSectionKind.AGGREGATE, formatter.kind());
    }

    @Test
    void format_deberiaIncluirTodosLosPasos() {
        var steps = List.of(
            NextStep.of(NextStep.SOURCE_RECOMMENDATION, "Validar con 10 clientes", 9, "confirmar demanda"),
            NextStep.of(NextStep.SOURCE_OPPORTUNITY, "Pilotar en un barrio", 7, "probar aceptación")
        );
        var section = new NextStepsSection(steps);

        var result = formatter.format(section);

        assertTrue(result.contains("## Próximos Pasos"));
        assertTrue(result.contains("### 1. Validar con 10 clientes"));
        assertTrue(result.contains("**Origen:** RECOMMENDATION"));
        assertTrue(result.contains("**Prioridad:** 9/10"));
        assertTrue(result.contains("**Razón:** confirmar demanda"));
        assertTrue(result.contains("### 2. Pilotar en un barrio"));
        assertTrue(result.contains("**Origen:** OPPORTUNITY"));
        assertTrue(result.contains("**Prioridad:** 7/10"));
        assertTrue(result.contains("**Razón:** probar aceptación"));
        assertTrue(result.contains("**Total:** 2 pasos siguientes"));
    }

    @Test
    void format_deberiaMostrarMensajeDefault_cuandoNoHayPasos() {
        var result = formatter.format(NextStepsSection.empty());

        assertTrue(result.contains("## Próximos Pasos"));
        assertTrue(result.contains("_Sin próximos pasos definidos._"));
    }
}
