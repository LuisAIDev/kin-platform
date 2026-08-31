package com.kinplatform.kin.health.aiassist.adapter;

import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_assist_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIAssistEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AIAssistType type;

    @Column(name = "input_data", nullable = false, columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "context")
    private String context;
}
