package com.kinplatform.kin.health.aiassist.adapter;

import com.kinplatform.kin.health.aiassist.domain.AIAssistRequest;
import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import com.kinplatform.kin.health.aiassist.port.AIAssistRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaAIAssistRepository implements AIAssistRepository {

    private final AIAssistJpaRepository repository;

    public JpaAIAssistRepository(AIAssistJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AIAssistRequest save(AIAssistRequest request) {
        AIAssistEntity entity = repository.findById(request.id()).orElseGet(AIAssistEntity::new);
        entity.setId(request.id());
        entity.setType(request.type());
        entity.setInputData(request.inputData());
        entity.setResponse(request.response());
        entity.setTimestamp(request.timestamp());
        entity.setUserId(request.userId());
        entity.setPatientId(request.patientId());
        entity.setContext(request.context());
        repository.save(entity);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AIAssistRequest> findById(UUID id) {
        return repository.findById(id).map(JpaAIAssistRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIAssistRequest> findByPatientId(UUID patientId) {
        if (patientId == null) return List.of();
        return repository.findByPatientIdOrderByTimestampDesc(patientId).stream()
                .map(JpaAIAssistRepository::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIAssistRequest> findByUserId(UUID userId) {
        if (userId == null) return List.of();
        return repository.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(JpaAIAssistRepository::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIAssistRequest> findByPatientIdAndType(UUID patientId, AIAssistType type) {
        if (patientId == null || type == null) return List.of();
        return repository.findByPatientIdAndTypeOrderByTimestampDesc(patientId, type).stream()
                .map(JpaAIAssistRepository::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByPatientId(UUID patientId) {
        if (patientId == null) return 0;
        return repository.countByPatientId(patientId);
    }

    static AIAssistRequest toDomain(AIAssistEntity e) {
        return AIAssistRequest.of(
                e.getType(), e.getInputData(), e.getResponse(),
                e.getUserId(), e.getPatientId(), e.getContext());
    }
}
