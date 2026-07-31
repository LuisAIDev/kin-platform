package com.kinplatform.ai.context.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que persiste el estado serializado del {@code ProjectContext} de un
 * proyecto (1:1 con {@code projects.id}).
 *
 * <p>El contexto se almacena como JSON en {@code context_data}; la
 * serialización/deserialización es responsabilidad del adaptador
 * ({@link JpaContextRepository}), manteniendo la entidad agnóstica del dominio.</p>
 */
@Entity
@Table(name = "project_context")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectContextEntity {

    @Id
    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "context_data", nullable = false, columnDefinition = "TEXT")
    private String contextData;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
