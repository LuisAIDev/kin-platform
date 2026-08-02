package com.kinplatform.kin.interview;

import com.kinplatform.kin.context.AnalyzedDimension;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewInputTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private static InterviewRequest request() {
        var context = InterviewContext.ofProject(PROJECT_ID);
        return InterviewRequest.of(context, null, InterviewState.empty(PROJECT_ID));
    }

    @Test
    void of_deberiaExponerCampos() {
        var request = request();
        var input = InterviewInput.of(request, "Nuestra solución es una app móvil");

        assertEquals(request, input.request());
        assertEquals("Nuestra solución es una app móvil", input.userMessage());
    }

    @Test
    void constructor_deberiaValidarRequest() {
        assertThrows(IllegalArgumentException.class, () -> new InterviewInput(null, "mensaje"));
    }

    @Test
    void constructor_deberiaNormalizarMensajeNulo() {
        var input = new InterviewInput(request(), null);

        assertEquals("", input.userMessage());
    }

    @Test
    void deberiaImplementarEngineInput() {
        assertEquals(1, InterviewInput.class.getInterfaces().length);
        assertEquals("EngineInput", InterviewInput.class.getInterfaces()[0].getSimpleName());
    }

    @Test
    void deberiaSoportarIgualdadHashCodeYToString() {
        var a = InterviewInput.of(request(), "mensaje");
        var b = InterviewInput.of(request(), "mensaje");
        var c = InterviewInput.of(request(), "otro");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "otro");
        assertTrue(a.toString().contains("userMessage=mensaje"));
    }

    @Test
    void contextoYDimensiones_deberianSerCompatibles() {
        var context = InterviewContext.of(PROJECT_ID, "Proyecto", "Software",
            Set.of(AnalyzedDimension.SECTOR, AnalyzedDimension.REVENUE_MODEL));
        var request = InterviewRequest.of(context, null, InterviewState.empty(PROJECT_ID));
        var input = InterviewInput.of(request, "");

        assertEquals(context, input.request().context());
        assertEquals(2, input.request().context().coveredDimensions().size());
    }
}
