package com.kinplatform.billing.authorization;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!prod")
public class MipresStubClient implements MipresClient {

    private final ConcurrentHashMap<String, MipresClient.AuthorizationData> mockData = new ConcurrentHashMap<>();

    public MipresStubClient() {
        mockData.put("AUTH-2024-001", new MipresClient.AuthorizationData("AUTH-2024-001", "890201", 10, new java.math.BigDecimal("45000"), "I10"));
        mockData.put("AUTH-2024-002", new MipresClient.AuthorizationData("AUTH-2024-002", "890301", 5, new java.math.BigDecimal("180000"), "K59"));
        mockData.put("AUTH-2024-003", new MipresClient.AuthorizationData("AUTH-2024-003", "890401", 20, new java.math.BigDecimal("25000"), "E78"));
    }

    @Override
    public Optional<MipresClient.AuthorizationData> consultarAutorizacion(String authorizationNumber, UUID contractId) {
        return Optional.ofNullable(mockData.get(authorizationNumber));
    }

    @Override
    public MipresClient.ConsumptionResult reportarUso(String authorizationNumber, UUID contractId, String cupsCode, int quantity, java.math.BigDecimal value) {
        MipresClient.AuthorizationData data = mockData.get(authorizationNumber);
        if (data == null) {
            return new MipresClient.ConsumptionResult(false, "Autorización no encontrada");
        }
        if (!data.cupsCode().equals(cupsCode)) {
            return new MipresClient.ConsumptionResult(false, "CUPS no coincide con autorización");
        }
        return new MipresClient.ConsumptionResult(true, null);
    }

    @Override
    public List<String> listarAutorizacionesVigentes(UUID contractId, UUID patientId) {
        return mockData.keySet().stream().toList();
    }

    // Helper para tests
    void addMockAuth(String authNumber, MipresClient.AuthorizationData data) {
        mockData.put(authNumber, data);
    }
}