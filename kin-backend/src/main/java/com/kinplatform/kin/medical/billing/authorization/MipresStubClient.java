package com.kinplatform.kin.medical.billing.authorization;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!prod")
public class MipresStubClient implements MipresClient {

    private final ConcurrentHashMap<String, MipresClient.AuthorizationData> mockAuthData = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> mockTokens = new ConcurrentHashMap<>();

    public MipresStubClient() {
        // Seed data
        mockAuthData.put("AUTH-2024-001",
                new MipresClient.AuthorizationData("AUTH-2024-001", "890201", 10, new BigDecimal("45000"), "I10"));
        mockAuthData.put("AUTH-2024-002",
                new MipresClient.AuthorizationData("AUTH-2024-002", "890301", 5, new BigDecimal("180000"), "K59"));
        mockAuthData.put("AUTH-2024-003",
                new MipresClient.AuthorizationData("AUTH-2024-003", "890401", 20, new BigDecimal("25000"), "E78"));

        // Fake token
        mockTokens.put("TEST-NIT", "STUB-TOKEN-" + OffsetDateTime.now().plusHours(24).toEpochSecond());
    }

    @Override
    public Optional<AuthorizationData> consultarAutorizacion(String authorizationNumber, UUID contractId) {
        return Optional.ofNullable(mockAuthData.get(authorizationNumber));
    }

    @Override
    public ConsumptionResult reportarUso(String authorizationNumber, UUID contractId,
                                          String cupsCode, int quantity, BigDecimal value) {
        AuthorizationData data = mockAuthData.get(authorizationNumber);
        if (data == null) {
            return new ConsumptionResult(false, "Autorización no encontrada: " + authorizationNumber);
        }
        if (!data.cupsCode().equals(cupsCode)) {
            return new ConsumptionResult(false, "CUPS no coincide con autorización: " + cupsCode);
        }
        if (quantity <= 0) {
            return new ConsumptionResult(false, "Cantidad debe ser mayor a 0");
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return new ConsumptionResult(false, "Valor debe ser mayor a 0");
        }

        // Simular reporte exitoso
        return new ConsumptionResult(true, null);
    }

    @Override
    public List<String> listarAutorizacionesVigentes(UUID contractId, UUID patientId) {
        return mockAuthData.keySet().stream().toList();
    }

    // Helpers para tests
    void addMockAuth(String authNumber, AuthorizationData data) {
        mockAuthData.put(authNumber, data);
    }

    void clearMockData() {
        mockAuthData.clear();
        // Re-seed
        mockAuthData.put("AUTH-2024-001",
                new AuthorizationData("AUTH-2024-001", "890201", 10, new BigDecimal("45000"), "I10"));
        mockAuthData.put("AUTH-2024-002",
                new AuthorizationData("AUTH-2024-002", "890301", 5, new BigDecimal("180000"), "K59"));
        mockAuthData.put("AUTH-2024-003",
                new AuthorizationData("AUTH-2024-003", "890401", 20, new BigDecimal("25000"), "E78"));
    }

    String getFakeToken(String nit) {
        return mockTokens.getOrDefault(nit, "STUB-TOKEN-DEFAULT");
    }
}
