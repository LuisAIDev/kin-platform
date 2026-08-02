package com.kinplatform.ai.interview.adapter;

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
 * Entidad que persiste el estado serializado de la entrevista estratégica de un
 * proyecto (1:1 con {@code projects.id}, ADR-015).
 *
 * <p>El estado se almacena como JSON en {@code state_data}; la
 * serialización/deserialización es responsabilidad del adaptador
 * ({@link JpaInterviewRepository}) vía {@link InterviewStateMapper}, manteniendo
 * la entidad agnóstica del dominio.</p>
 */
@Entity
@Table(name = "interview_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewStateEntity {

    @Id
    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "state_data", nullable = false, columnDefinition = "TEXT")
    private String stateData;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
