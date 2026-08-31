package com.kinplatform.kin.health.aiassist.adapter;

import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIAssistJpaRepository extends JpaRepository<AIAssistEntity, UUID> {

    List<AIAssistEntity> findByPatientIdOrderByTimestampDesc(UUID patientId);

    List<AIAssistEntity> findByUserIdOrderByTimestampDesc(UUID userId);

    List<AIAssistEntity> findByPatientIdAndTypeOrderByTimestampDesc(UUID patientId, AIAssistType type);

    long countByPatientId(UUID patientId);
}
