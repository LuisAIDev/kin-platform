package com.kinplatform.platform.enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.projectinfo.StructuredInfoSourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResolvedContextTest {

    @Test
    void usuarioPrevaleceSobreDocumento() {
        List<StructuredDatum> data = List.of(
                new StructuredDatum(
                        "MERCADO", "competidores", "Competidor confirmado", StructuredInfoSourceType.USER_INPUT),
                new StructuredDatum(
                        "MERCADO",
                        "competidores",
                        "Competidor del documento",
                        StructuredInfoSourceType.IMPORTED_DOCUMENT));

        ResolvedContext resolved = ResolvedContext.resolve(null, data);

        ResolvedValue competition = resolved.value(AnalyzedDimension.COMPETITION);
        assertThat(competition.value()).isEqualTo("Competidor confirmado");
        assertThat(competition.sourceType()).isEqualTo(StructuredInfoSourceType.USER_INPUT);
        assertThat(competition.state()).isEqualTo(DataState.CONFIRMED);
    }

    @Test
    void documentoImportadoNoSeConvierteEnUsuario() {
        List<StructuredDatum> data = List.of(new StructuredDatum(
                "MERCADO", "competidores", "Competidor A", StructuredInfoSourceType.IMPORTED_DOCUMENT));

        ResolvedContext resolved = ResolvedContext.resolve(null, data);

        ResolvedValue competition = resolved.value(AnalyzedDimension.COMPETITION);
        assertThat(competition.sourceType()).isEqualTo(StructuredInfoSourceType.IMPORTED_DOCUMENT);
        assertThat(competition.state()).isEqualTo(DataState.IMPORTED);
    }

    @Test
    void contextoConfirmadoNoSeSobrescribeYRegistraConflicto() {
        ProjectContext context = ProjectContext.fromProject("Titulo", "Solución del chat", "TECH");
        List<StructuredDatum> data = List.of(new StructuredDatum(
                "DATOS_GENERALES", "descripcion", "Solución del formulario", StructuredInfoSourceType.USER_INPUT));

        ResolvedContext resolved = ResolvedContext.resolve(context, data);

        assertThat(resolved.value(AnalyzedDimension.SOLUTION).value()).isEqualTo("Solución del chat");
        assertThat(resolved.value(AnalyzedDimension.SOLUTION).sourceType())
                .isEqualTo(StructuredInfoSourceType.USER_INPUT);
        assertThat(resolved.conflicts()).isNotEmpty();
    }

    @Test
    void origenCalculadoEstimadoIaSePreserva() {
        List<StructuredDatum> data = List.of(
                new StructuredDatum("MERCADO", "competidores", "Sugerencia IA", StructuredInfoSourceType.AI_SUGGESTED),
                new StructuredDatum("OPERACION", "empleados", "12", StructuredInfoSourceType.ESTIMATED));

        ResolvedContext resolved = ResolvedContext.resolve(null, data);

        assertThat(resolved.value(AnalyzedDimension.COMPETITION).state()).isEqualTo(DataState.AI_SUGGESTED);
        assertThat(resolved.value(AnalyzedDimension.RESOURCES).state()).isEqualTo(DataState.ESTIMATED);
    }

    @Test
    void datoAusenteEsPendienteNuncaCero() {
        ResolvedContext resolved = ResolvedContext.resolve(null, List.of());

        ResolvedValue mvp = resolved.value(AnalyzedDimension.MVP);
        assertThat(mvp.state()).isEqualTo(DataState.PENDING);
        assertThat(mvp.value()).isNull();
        assertThat(mvp.value()).isNotEqualTo("0");
    }

    @Test
    void regresionContextoSoloPreservaLas14Dimensiones() {
        ProjectContext context = ProjectContext.fromProject("KIN SaaS", "Plataforma SaaS", "TECH");
        ResolvedContext resolved = ResolvedContext.resolve(context, List.of());

        assertThat(resolved.all()).hasSize(AnalyzedDimension.values().length);
        assertThat(resolved.value(AnalyzedDimension.PROJECT_NAME).value()).isEqualTo("KIN SaaS");
        assertThat(resolved.value(AnalyzedDimension.SECTOR).value()).isEqualTo("TECH");
        assertThat(resolved.value(AnalyzedDimension.SOLUTION).value()).isEqualTo("Plataforma SaaS");
        assertThat(resolved.value(AnalyzedDimension.MVP).state()).isEqualTo(DataState.PENDING);
    }

    @Test
    void enterpriseSigueFuncionandoSoloConProjectContext() {
        ProjectContext context = ProjectContext.fromProject("Negocio", "Descripción", "SERVICIOS");
        ResolvedContext resolved = ResolvedContext.resolve(context, List.of());

        assertThat(resolved.value(AnalyzedDimension.PROJECT_NAME).state()).isEqualTo(DataState.CONFIRMED);
        assertThat(resolved.value(AnalyzedDimension.SECTOR).state()).isEqualTo(DataState.CONFIRMED);
        assertThat(resolved.conflicts()).isEmpty();
    }
}


