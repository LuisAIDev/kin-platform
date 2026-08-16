package com.kinplatform.ai.usage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio JPA de {@link AiUsageEntity}. La reserva usa SQL nativo atómico. */
public interface AiUsageJpaRepository extends JpaRepository<AiUsageEntity, UUID> {

    Optional<AiUsageEntity> findByUserIdAndPeriodStart(UUID userId, OffsetDateTime periodStart);

    /**
     * Crea la fila de período si no existe (idempotente). No falla si otra
     * transacción la creó primero.
     */
    @Modifying
    @Query(
            value = "INSERT INTO ai_usage "
                    + "(id, user_id, period_start, period_end, created_at, updated_at) "
                    + "VALUES (gen_random_uuid(), :userId, :periodStart, :periodEnd, now(), now()) "
                    + "ON CONFLICT (user_id, period_start) DO NOTHING",
            nativeQuery = true)
    void insertPeriodRowIfMissing(
            @Param("userId") UUID userId,
            @Param("periodStart") OffsetDateTime periodStart,
            @Param("periodEnd") OffsetDateTime periodEnd);

    /**
     * Reserva ATÓMICA de presupuesto. Actualiza exactamente una fila solo si
     * {@code consumo_actual + reserva_actual + estimación <= presupuesto}.
     * Dos requests concurrentes se serializan con el row lock de PostgreSQL:
     * el segundo ve la reserva del primero y falla la condición.
     *
     * @return número de filas actualizadas (0 = presupuesto insuficiente).
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "UPDATE ai_usage SET "
                    + "reserved_cost_usd = reserved_cost_usd + :estimate, "
                    + "request_count = request_count + 1, "
                    + "updated_at = now() "
                    + "WHERE user_id = :userId AND period_start = :periodStart "
                    + "AND estimated_cost_usd + reserved_cost_usd + :estimate <= :budget",
            nativeQuery = true)
    int tryReserve(
            @Param("userId") UUID userId,
            @Param("periodStart") OffsetDateTime periodStart,
            @Param("budget") BigDecimal budget,
            @Param("estimate") BigDecimal estimate);

    /**
     * Reconciliación de reserva con uso real: consume el costo real, libera la
     * reserva asociada y acumula tokens.
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "UPDATE ai_usage SET "
                    + "estimated_cost_usd = estimated_cost_usd + :actual, "
                    + "reserved_cost_usd = GREATEST(reserved_cost_usd - :reservation, 0), "
                    + "input_tokens = input_tokens + :inputTokens, "
                    + "output_tokens = output_tokens + :outputTokens, "
                    + "total_tokens = total_tokens + :inputTokens + :outputTokens, "
                    + "updated_at = now() "
                    + "WHERE user_id = :userId AND period_start = :periodStart",
            nativeQuery = true)
    void recordActual(
            @Param("userId") UUID userId,
            @Param("periodStart") OffsetDateTime periodStart,
            @Param("reservation") BigDecimal reservation,
            @Param("actual") BigDecimal actual,
            @Param("inputTokens") long inputTokens,
            @Param("outputTokens") long outputTokens);
}
