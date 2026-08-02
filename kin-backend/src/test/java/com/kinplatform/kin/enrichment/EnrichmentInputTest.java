package com.kinplatform.kin.enrichment;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrichmentInputTest {

    private ProjectContext context() {
        return ProjectContext.fromProject("Tienda online", "Venta minorista", "Retail");
    }

    private KnowledgeResult knowledge() {
        return KnowledgeResult.empty();
    }

    @Test
    void of_deberiaUsarTodasLasCategoriasYUmbralCeroPorDefecto() {
        var input = EnrichmentInput.of(context(), knowledge());

        assertEquals(4, input.categories().size());
        assertEquals(0.0, input.minScore(), 1e-9);
    }

    @Test
    void deberiaNormalizarCategoriasNulasATodas() {
        var input = new EnrichmentInput(context(), knowledge(), null, 0.0);

        assertEquals(4, input.categories().size());
    }

    @Test
    void deberiaNormalizarCategoriasVaciasATodas() {
        var input = new EnrichmentInput(context(), knowledge(), Set.of(), 0.0);

        assertEquals(4, input.categories().size());
    }

    @Test
    void deberiaConservarSoloLasCategoriasIndicadas() {
        var input = EnrichmentInput.of(context(), knowledge(),
            Set.of(EvidenceCategory.MARKET, EvidenceCategory.FINANCIAL), 0.4);

        assertEquals(2, input.categories().size());
        assertTrue(input.categories().contains(EvidenceCategory.MARKET));
        assertTrue(input.categories().contains(EvidenceCategory.FINANCIAL));
        assertFalse(input.categories().contains(EvidenceCategory.INNOVATION));
        assertEquals(0.4, input.minScore(), 1e-9);
    }

    @Test
    void deberiaAcotarElUmbralMinimo() {
        var input = new EnrichmentInput(context(), knowledge(), null, -0.5);
        assertEquals(0.0, input.minScore(), 1e-9);

        var input2 = new EnrichmentInput(context(), knowledge(), null, 1.7);
        assertEquals(1.0, input2.minScore(), 1e-9);
    }

    @Test
    void deberiaSoportarContextoYConocimientoNulos() {
        var input = new EnrichmentInput(null, null, null, 0.0);

        assertNull(input.context());
        assertNull(input.knowledge());
        assertEquals(EnumSet.allOf(EvidenceCategory.class), input.categories());
    }
}
