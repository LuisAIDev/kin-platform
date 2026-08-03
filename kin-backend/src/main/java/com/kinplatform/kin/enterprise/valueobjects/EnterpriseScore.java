package com.kinplatform.kin.enterprise.valueobjects;

import java.util.Objects;

/**
 * Enterprise Score del proyecto empresarial (value object).
 *
 * <p>Puntuación empresarial multidimensional con exactamente las ocho
 * dimensiones aprobadas: {@code Market}, {@code Innovation}, {@code Viability},
 * {@code Financial}, {@code Risk}, {@code Scalability}, {@code Team} y
 * {@code Sustainability}. Cada dimensión se puntúa entre 0 y 100. Además
 * agrega la puntuación global ({@code overallScore}), la confianza
 * (0-1) y el grado ({@link ScoreGrade}) derivado de la puntuación global.
 * Producido por {@code EnterpriseScoreEngine} de forma totalmente
 * determinista.</p>
 *
 * <p>Clase final inmutable: el constructor es privado y las instancias solo se
 * crean mediante las factories {@link #empty()} y {@link #calculate(double,
 * double, double, double, double, double, double, double, double)}.</p>
 */
public final class EnterpriseScore {

    private final double market;
    private final double innovation;
    private final double viability;
    private final double financial;
    private final double risk;
    private final double scalability;
    private final double team;
    private final double sustainability;
    private final int overallScore;
    private final double confidence;
    private final ScoreGrade grade;

    private EnterpriseScore(double market, double innovation, double viability,
                            double financial, double risk, double scalability,
                            double team, double sustainability,
                            int overallScore, double confidence, ScoreGrade grade) {
        this.market = requireScore(market, "market");
        this.innovation = requireScore(innovation, "innovation");
        this.viability = requireScore(viability, "viability");
        this.financial = requireScore(financial, "financial");
        this.risk = requireScore(risk, "risk");
        this.scalability = requireScore(scalability, "scalability");
        this.team = requireScore(team, "team");
        this.sustainability = requireScore(sustainability, "sustainability");
        this.overallScore = ValueObjects.requireInRange(overallScore, 0, 100, "overallScore");
        this.confidence = ValueObjects.requireInRange(confidence, 0.0, 1.0, "confidence");
        if (grade == null) {
            throw new IllegalArgumentException("'grade' no puede ser null.");
        }
        if (grade != ScoreGrade.from(overallScore)) {
            throw new IllegalArgumentException("'grade' no coincide con 'overallScore'.");
        }
        this.grade = grade;
    }

    private static double requireScore(double value, String field) {
        return ValueObjects.requireInRange(value, 0.0, 100.0, field);
    }

    /**
     * Crea un Enterprise Score vacío: todas las dimensiones en cero, con grado
     * {@code CRITICAL} y confianza cero.
     */
    public static EnterpriseScore empty() {
        return new EnterpriseScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0, 0.0, ScoreGrade.from(0));
    }

    /**
     * Calcula el Enterprise Score a partir de las ocho dimensiones.
     *
     * <p>La puntuación global es la media redondeada de las ocho dimensiones y
     * el grado se deriva automáticamente de dicha media.</p>
     *
     * @param market        puntuación de la dimensión de mercado (0-100)
     * @param innovation    puntuación de la dimensión de innovación (0-100)
     * @param viability     puntuación de la dimensión de viabilidad (0-100)
     * @param financial     puntuación de la dimensión financiera (0-100)
     * @param risk          puntuación de la dimensión de riesgo (0-100)
     * @param scalability   puntuación de la dimensión de escalabilidad (0-100)
     * @param team          puntuación de la dimensión de equipo (0-100)
     * @param sustainability puntuación de la dimensión de sostenibilidad (0-100)
     * @param confidence    confianza de la puntuación (0-1)
     * @return Enterprise Score calculado
     */
    public static EnterpriseScore calculate(double market, double innovation, double viability,
                                            double financial, double risk, double scalability,
                                            double team, double sustainability,
                                            double confidence) {
        double average = (market + innovation + viability + financial + risk
            + scalability + team + sustainability) / 8.0;
        int overall = (int) Math.round(average);
        return new EnterpriseScore(market, innovation, viability, financial, risk,
            scalability, team, sustainability, overall, confidence, ScoreGrade.from(overall));
    }

    public double market() {
        return market;
    }

    public double innovation() {
        return innovation;
    }

    public double viability() {
        return viability;
    }

    public double financial() {
        return financial;
    }

    public double risk() {
        return risk;
    }

    public double scalability() {
        return scalability;
    }

    public double team() {
        return team;
    }

    public double sustainability() {
        return sustainability;
    }

    public int overallScore() {
        return overallScore;
    }

    public double confidence() {
        return confidence;
    }

    public ScoreGrade grade() {
        return grade;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnterpriseScore that)) {
            return false;
        }
        return Double.compare(that.market, market) == 0
            && Double.compare(that.innovation, innovation) == 0
            && Double.compare(that.viability, viability) == 0
            && Double.compare(that.financial, financial) == 0
            && Double.compare(that.risk, risk) == 0
            && Double.compare(that.scalability, scalability) == 0
            && Double.compare(that.team, team) == 0
            && Double.compare(that.sustainability, sustainability) == 0
            && overallScore == that.overallScore
            && Double.compare(that.confidence, confidence) == 0
            && grade == that.grade;
    }

    @Override
    public int hashCode() {
        return Objects.hash(market, innovation, viability, financial, risk,
            scalability, team, sustainability, overallScore, confidence, grade);
    }

    @Override
    public String toString() {
        return "EnterpriseScore[overall=" + overallScore + ", grade=" + grade
            + ", confidence=" + confidence + ", market=" + market
            + ", innovation=" + innovation + ", viability=" + viability
            + ", financial=" + financial + ", risk=" + risk
            + ", scalability=" + scalability + ", team=" + team
            + ", sustainability=" + sustainability + "]";
    }
}
