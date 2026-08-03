package com.kinplatform.ai.enterprise.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Value object embebido del Enterprise Score (Fase 10, Milestone 2G).
 *
 * <p>Persiste el {@code EnterpriseScore} multidimensional dentro de la fila de
 * {@link EnterpriseProjectEntity} (1:1 con la versión, sin joins adicionales).
 * Las ocho dimensiones, la puntuación global, la confianza y el grado se
 * almacenan en columnas {@code score_*} opcionales: una versión puede
 * persistirse sin score (p. ej. {@code REQUESTED} o {@code FAILED} sin
 * puntuación).</p>
 *
 * <p>Clase de infraestructura agnóstica del dominio: el mapeador
 * {@link EnterpriseScoreMapper} convierte {@code EnterpriseScore} a esta forma
 * y viceversa, de modo que la entidad no conoce el value object de dominio.</p>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseScoreEntity {

    @Column(name = "score_market")
    private Double market;

    @Column(name = "score_innovation")
    private Double innovation;

    @Column(name = "score_viability")
    private Double viability;

    @Column(name = "score_financial")
    private Double financial;

    @Column(name = "score_risk")
    private Double risk;

    @Column(name = "score_scalability")
    private Double scalability;

    @Column(name = "score_team")
    private Double team;

    @Column(name = "score_sustainability")
    private Double sustainability;

    @Column(name = "score_overall")
    private Integer overall;

    @Column(name = "score_confidence")
    private Double confidence;

    @Column(name = "score_grade", length = 16)
    private String grade;
}
