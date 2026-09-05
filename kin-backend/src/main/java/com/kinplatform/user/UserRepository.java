package com.kinplatform.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Médicos con un estado de verificación concreto (p. ej. PENDING para el panel de ADMIN). */
    List<User> findByRoleAndPhysicianVerificationStatus(UserRole role, PhysicianVerificationStatus verificationStatus);

    /**
     * Usuarios con una solicitud profesional en el estado indicado,
     * INDEPENDIENTEMENTE de su persona/rol ({@code users.role}). Permite al
     * panel de ADMIN revisar solicitudes de capacidad PHYSICIAN presentadas
     * por cuentas existentes (FREE/PREMIUM/PATIENT/…) sin exigir
     * {@code role=PHYSICIAN} (Alternativa B: la capacidad se desacopla del rol).
     */
    List<User> findByPhysicianVerificationStatus(PhysicianVerificationStatus verificationStatus);

    /**
     * Incremento ATÓMICO y CONDICIONAL del contador persistente de proyectos
     * completados. Solo incrementa si el usuario no supera el límite
     * ({@code limit} {@code null} = ilimitado). Nunca se decrementa al
     * eliminar proyectos (evita el abuso crear→completar→eliminar→crear).
     *
     * @return número de filas actualizadas (1 = cupo consumido, 0 = límite).
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "UPDATE users SET completed_projects = completed_projects + 1, "
                    + "updated_at = now() WHERE id = :userId "
                    + "AND (:limit IS NULL OR completed_projects < :limit)",
            nativeQuery = true)
    int tryIncrementCompletedProjects(@Param("userId") UUID userId, @Param("limit") Integer limit);

    /**
     * Rollover mensual: si el período almacenado no es el vigente, reinicia el
     * contador. Idempotente y atómico (solo actualiza cuando corresponde).
     *
     * @return número de filas actualizadas (1 si hubo rollover, 0 si no).
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "UPDATE users SET completed_projects = 0, "
                    + "completed_projects_period_start = :periodStart, updated_at = now() "
                    + "WHERE id = :userId AND "
                    + "(completed_projects_period_start IS NULL OR completed_projects_period_start <> :periodStart)",
            nativeQuery = true)
    int rolloverCompletedProjects(@Param("userId") UUID userId, @Param("periodStart") OffsetDateTime periodStart);
}
