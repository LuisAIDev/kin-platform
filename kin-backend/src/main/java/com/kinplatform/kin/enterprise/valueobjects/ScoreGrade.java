package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Grado o calificación del Enterprise Score (value object).
 *
 * <p>Traduce el {@code overallScore} (0-100) a una etiqueta de nivel:
 * {@code EXCELLENT} (90+), {@code GOOD} (75-89), {@code FAIR} (60-74),
 * {@code WEAK} (40-59) o {@code CRITICAL} (&lt;40). Usado por
 * {@link EnterpriseScore}.</p>
 */
public enum ScoreGrade {
    EXCELLENT,
    GOOD,
    FAIR,
    WEAK,
    CRITICAL;

    /**
     * Deriva el grado a partir de la puntuación global.
     */
    public static ScoreGrade from(int overallScore) {
        if (overallScore >= 90) {
            return EXCELLENT;
        }
        if (overallScore >= 75) {
            return GOOD;
        }
        if (overallScore >= 60) {
            return FAIR;
        }
        if (overallScore >= 40) {
            return WEAK;
        }
        return CRITICAL;
    }
}
