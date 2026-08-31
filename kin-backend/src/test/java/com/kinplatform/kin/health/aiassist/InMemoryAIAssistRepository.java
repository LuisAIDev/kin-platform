package com.kinplatform.kin.health.aiassist;

import com.kinplatform.kin.health.aiassist.domain.AIAssistRequest;
import com.kinplatform.kin.health.aiassist.domain.AIAssistType;
import com.kinplatform.kin.health.aiassist.port.AIAssistRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryAIAssistRepository implements AIAssistRepository {

    private final Map<UUID, AIAssistRequest> requests = new ConcurrentHashMap<>();

    @Override
    public AIAssistRequest save(AIAssistRequest request) {
        requests.put(request.id(), request);
        return request;
    }

    @Override
    public Optional<AIAssistRequest> findById(UUID id) {
        return Optional.ofNullable(requests.get(id));
    }

    @Override
    public List<AIAssistRequest> findByPatientId(UUID patientId) {
        return requests.values().stream()
                .filter(r -> r.patientId().equals(patientId))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
    }

    @Override
    public List<AIAssistRequest> findByUserId(UUID userId) {
        return requests.values().stream()
                .filter(r -> r.userId().equals(userId))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
    }

    @Override
    public List<AIAssistRequest> findByPatientIdAndType(UUID patientId, AIAssistType type) {
        return requests.values().stream()
                .filter(r -> r.patientId().equals(patientId) && r.type().equals(type))
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();
    }

    @Override
    public long countByPatientId(UUID patientId) {
        return requests.values().stream()
                .filter(r -> r.patientId().equals(patientId))
                .count();
    }

    public Map<UUID, AIAssistRequest> all() {
        return Map.copyOf(requests);
    }
}
