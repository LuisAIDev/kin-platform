package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnterpriseScoreMapperTest {

    private final EnterpriseScoreMapper mapper = new EnterpriseScoreMapper();

    @Test
    void roundTrip_conScoreCompleto_esExacto() {
        var original = EnterprisePersistenceTestFixtures.score();

        var entity = mapper.toEmbedded(original);
        var restored = mapper.toDomain(entity);

        assertEquals(original, restored);
        assertEquals(original.overallScore(), entity.getOverall());
        assertEquals(original.grade().name(), entity.getGrade());
        assertEquals(original.confidence(), entity.getConfidence());
    }

    @Test
    void toEmbedded_conScoreNulo_devuelveNull() {
        assertNull(mapper.toEmbedded(null));
    }

    @Test
    void toDomain_conEntidadNula_devuelveNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toEmbedded_mantieneLasOchoDimensiones() {
        var score = EnterpriseScore.calculate(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 0.5);

        var entity = mapper.toEmbedded(score);

        assertEquals(10.0, entity.getMarket());
        assertEquals(20.0, entity.getInnovation());
        assertEquals(30.0, entity.getViability());
        assertEquals(40.0, entity.getFinancial());
        assertEquals(50.0, entity.getRisk());
        assertEquals(60.0, entity.getScalability());
        assertEquals(70.0, entity.getTeam());
        assertEquals(80.0, entity.getSustainability());
    }
}
