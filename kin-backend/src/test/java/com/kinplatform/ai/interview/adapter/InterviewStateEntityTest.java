package com.kinplatform.ai.interview.adapter;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewStateEntityTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String STATE_DATA = "{\"answers\":{}}";
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.now();

    @Test
    void noArgsConstructor_deberiaCrearEntidadVacia() {
        var entity = new InterviewStateEntity();

        assertNotNull(entity);
        assertNull(entity.getProjectId());
        assertNull(entity.getStateData());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void allArgsConstructor_deberiaInicializarTodosLosCampos() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        assertEquals(PROJECT_ID, entity.getProjectId());
        assertEquals(STATE_DATA, entity.getStateData());
        assertSame(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void setters_deberianActualizarLosCampos() {
        var entity = new InterviewStateEntity();
        entity.setProjectId(PROJECT_ID);
        entity.setStateData(STATE_DATA);
        entity.setUpdatedAt(UPDATED_AT);

        assertEquals(PROJECT_ID, entity.getProjectId());
        assertEquals(STATE_DATA, entity.getStateData());
        assertSame(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void builder_deberiaConstruirEntidadCompleta() {
        var entity = InterviewStateEntity.builder()
            .projectId(PROJECT_ID)
            .stateData(STATE_DATA)
            .updatedAt(UPDATED_AT)
            .build();

        assertEquals(PROJECT_ID, entity.getProjectId());
        assertEquals(STATE_DATA, entity.getStateData());
        assertSame(UPDATED_AT, entity.getUpdatedAt());
    }

    @Test
    void builder_sinCamposObligatorios_deberiaConstruirEntidadConNulos() {
        var entity = InterviewStateEntity.builder().build();

        assertNull(entity.getProjectId());
        assertNull(entity.getStateData());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void equals_deberiaConsiderarIgualesEntidadesIdenticas() {
        var a = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);
        var b = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_conMismoObjeto_deberiaSerTrue() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        assertEquals(entity, entity);
    }

    @Test
    void equals_conNull_deberiaSerFalse() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        assertNotEquals(null, entity);
    }

    @Test
    void equals_conOtraClase_deberiaSerFalse() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        assertFalse(entity.equals("not-an-entity"));
    }

    @Test
    void equals_conProjectIdDistinto_deberiaSerFalse() {
        var a = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);
        var b = new InterviewStateEntity(UUID.randomUUID(), STATE_DATA, UPDATED_AT);

        assertNotEquals(a, b);
    }

    @Test
    void equals_conStateDataDistinto_deberiaSerFalse() {
        var a = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);
        var b = new InterviewStateEntity(PROJECT_ID, "{\"answers\":{\"q\":1}}", UPDATED_AT);

        assertNotEquals(a, b);
    }

    @Test
    void equals_conUpdatedAtDistinto_deberiaSerFalse() {
        var a = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);
        var b = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT.plusSeconds(10));

        assertNotEquals(a, b);
    }

    @Test
    void equals_conEntidadesNulas_deberiaCompararCorrectamente() {
        var allNull = new InterviewStateEntity();
        var otherNull = new InterviewStateEntity();

        assertEquals(allNull, otherNull);
        assertNotEquals(allNull, new InterviewStateEntity(PROJECT_ID, null, null));
    }

    @Test
    void hashCode_deberiaSerEstableParaEntidadesIguales() {
        var a = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);
        var b = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_deberiaIncluirLosCampos() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        var result = entity.toString();

        assertTrue(result.contains(PROJECT_ID.toString()));
        assertTrue(result.contains(STATE_DATA));
        assertTrue(result.contains(UPDATED_AT.toString()));
    }

    @Test
    void touch_deberiaActualizarUpdatedAt() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, UPDATED_AT);

        entity.touch();

        assertNotNull(entity.getUpdatedAt());
        assertTrue(!entity.getUpdatedAt().isBefore(UPDATED_AT));
    }

    @Test
    void touch_conUpdatedAtNulo_deberiaAsignarFecha() {
        var entity = new InterviewStateEntity(PROJECT_ID, STATE_DATA, null);

        entity.touch();

        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void builder_deberiaSoportarMetodosIndividuales() {
        var entity = InterviewStateEntity.builder()
            .projectId(PROJECT_ID)
            .build();

        assertEquals(PROJECT_ID, entity.getProjectId());
        assertNull(entity.getStateData());
        assertNull(entity.getUpdatedAt());
    }
}
