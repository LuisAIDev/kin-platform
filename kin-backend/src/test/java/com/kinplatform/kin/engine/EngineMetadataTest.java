package com.kinplatform.kin.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EngineMetadataTest {

    @Test
    void of_deberiaCrearMetadatosSinDependencias() {
        var metadata = EngineMetadata.of("Engine", "v1", "Author",
            EnginePhase.RISK, EngineType.DOMAIN, 40);

        assertEquals("Engine", metadata.name());
        assertEquals("v1", metadata.version());
        assertEquals("Author", metadata.author());
        assertEquals(EnginePhase.RISK, metadata.phase());
        assertEquals(EngineType.DOMAIN, metadata.type());
        assertEquals(40, metadata.priority());
        assertTrue(metadata.dependencies().isEmpty());
    }

    @Test
    void constructor_deberiaDefenderDependenciasNulas() {
        var metadata = new EngineMetadata("Engine", "v1", "Author",
            EnginePhase.RISK, EngineType.DOMAIN, 40, null);
        assertTrue(metadata.dependencies().isEmpty());
    }

    @Test
    void dependsOn_deberiaDetectarDependencias() {
        var metadata = new EngineMetadata("Engine", "v1", "Author",
            EnginePhase.RISK, EngineType.DOMAIN, 40, List.of("ScoringEngine"));
        assertTrue(metadata.dependsOn("ScoringEngine"));
        assertFalse(metadata.dependsOn("RiskEngine"));
    }

    @Test
    void displayNames_deberianEstarDefinidos() {
        assertEquals("Riesgos", EnginePhase.RISK.displayName());
        assertEquals("Motor de dominio puro", EngineType.DOMAIN.description());
    }
}
