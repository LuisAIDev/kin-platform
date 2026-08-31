package com.kinplatform.kin.health.aiassist.port;

import com.kinplatform.kin.health.aiassist.domain.AIAssistRequest;
import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AIAssistRepository {

    AIAssistRequest save(AIAssistRequest request);

    Optional<AIAssistRequest> findById(UUID id);

    List<AIAssistRequest> findByPatientId(UUID patientId);

    List<AIAssistRequest> findByUserId(UUID userId);

    List<AIAssistRequest> findByPatientIdAndType(UUID patientId, AIAssistType type);

    long countByPatientId(UUID patientId);
}
