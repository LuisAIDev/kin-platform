package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;

/**
 * Mapeador del Enterprise Score (Fase 10, Milestone 2G).
 *
 * <p>Convierte el value object de dominio {@link EnterpriseScore} en la forma
 * embebida {@link EnterpriseScoreEntity} y viceversa, sin acoplar la
 * infraestructura JPA al dominio.</p>
 *
 * <p>La reconstrucción del dominio usa {@link EnterpriseScore#calculate(double,
 * double, double, double, double, double, double, double, double)} con las ocho
 * dimensiones y la confianza persistidas: la puntuación global y el grado se
 * derivan de forma determinista, por lo que coinciden exactamente con los
 * valores originales (que se almacenan además como instantánea consultable en
 * las columnas {@code score_overall}/{@code score_grade}).</p>
 */
public final class EnterpriseScoreMapper {

    /**
     * Convierte un score de dominio en la forma embebida.
     *
     * @param score score de dominio, o {@code null}
     * @return la forma embebida, o {@code null} si la entrada es {@code null}
     */
    public EnterpriseScoreEntity toEmbedded(EnterpriseScore score) {
        if (score == null) {
            return null;
        }
        return EnterpriseScoreEntity.builder()
            .market(score.market())
            .innovation(score.innovation())
            .viability(score.viability())
            .financial(score.financial())
            .risk(score.risk())
            .scalability(score.scalability())
            .team(score.team())
            .sustainability(score.sustainability())
            .overall(score.overallScore())
            .confidence(score.confidence())
            .grade(score.grade().name())
            .build();
    }

    /**
     * Reconstruye un score de dominio a partir de la forma embebida.
     *
     * @param entity forma embebida, o {@code null}
     * @return el score de dominio, o {@code null} si la entrada es {@code null}
     */
    public EnterpriseScore toDomain(EnterpriseScoreEntity entity) {
        if (entity == null) {
            return null;
        }
        return EnterpriseScore.calculate(
            entity.getMarket(), entity.getInnovation(), entity.getViability(),
            entity.getFinancial(), entity.getRisk(), entity.getScalability(),
            entity.getTeam(), entity.getSustainability(), entity.getConfidence());
    }
}
