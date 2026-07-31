package com.kinplatform.kin.ai;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptAssemblerTest {

    private final PromptAssembler assembler = new PromptAssembler();

    @Test
    void assemble_deberiaIncluirLosDatosDelProyecto() {
        var prompt = assembler.assemble("Mi App", "App de gestión", "Software", null);

        assertTrue(prompt.contains("Mi App"));
        assertTrue(prompt.contains("App de gestión"));
        assertTrue(prompt.contains("Software"));
        assertTrue(prompt.contains("KIN"));
    }

    @Test
    void assemble_deberiaUsarDescripcionDefault_cuandoNoHay() {
        var prompt = assembler.assemble("Mi App", null, "Software", null);
        assertTrue(prompt.contains("Sin descripción disponible."));
    }

    @Test
    void assemble_deberiaIncluirElSnippetDeContextoConocido() {
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        var prompt = assembler.assemble("Mi App", "App de gestión", "Software", ctx);

        assertTrue(prompt.contains("## INFORMACIÓN CONOCIDA DEL PROYECTO"));
        assertTrue(prompt.contains(ctx.toPromptSnippet().trim()));
    }

    @Test
    void assemble_deberiaIncluirLaInstruccionEstrategica_cuandoHayDecision() {
        var ctx = ProjectContext.fromProject("Mi App", "App de gestión", "Software");
        ctx.attachDecision(ConversationDecision.ask(AnalyzedDimension.MVP, 7, "explorar plan de validación"));
        var prompt = assembler.assemble("Mi App", "App de gestión", "Software", ctx);

        assertTrue(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
        assertTrue(prompt.contains("MVP"));
    }

    @Test
    void assemble_sinContextoNiDecision_noDebeIncluirSnippets() {
        var prompt = assembler.assemble("Mi App", "App de gestión", "Software", null);
        assertFalse(prompt.contains("## INFORMACIÓN CONOCIDA"));
        assertFalse(prompt.contains("## INSTRUCCIÓN ESTRATÉGICA"));
    }
}
