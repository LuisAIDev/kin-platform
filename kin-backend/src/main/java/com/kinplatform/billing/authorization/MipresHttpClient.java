package com.kinplatform.billing.authorization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class MipresHttpClient implements MipresClient {

    private final WebClient webClient;
    private final MipresTokenService tokenService;

    @Override
    public Optional<AuthorizationData> consultarAutorizacion(String authorizationNumber, UUID contractId) {
        String token = tokenService.getValidToken();
        String nit = getConfiguredNit();

        String url = String.format("%sapi/PrescripcionXNumero/%s/%s/%s",
                getBaseUrl(), nit, token, authorizationNumber);

        log.info("Consultando autorización MIPRES: {} para contrato {}", authorizationNumber, contractId);

        try {
            MipresPrescriptionResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(MipresPrescriptionResponse.class)
                    .block();

            if (response == null) {
                return Optional.empty();
            }

            if (contractId != null && response.contractId() != null
                    && !contractId.equals(response.contractId())) {
                log.warn("Autorización {} no pertenece al contrato {}", authorizationNumber, contractId);
                return Optional.empty();
            }

            AuthorizationData data = new AuthorizationData(
                    response.prescriptionNumber(),
                    response.cupsCode(),
                    response.qtyApproved(),
                    response.unitPriceCop(),
                    response.diagnosisCie10()
            );

            return Optional.of(data);

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Autorización MIPRES no encontrada: {}", authorizationNumber);
            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.error("Error HTTP consultando autorización MIPRES: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new MipresClientException("Error consultando autorización: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error consultando autorización MIPRES", e);
            throw new MipresClientException("Error consultando autorización: " + e.getMessage());
        }
    }

    @Override
    public ConsumptionResult reportarUso(String authorizationNumber, UUID contractId,
                                          String cupsCode, int quantity, BigDecimal value) {
        String token = tokenService.getValidToken();
        String nit = getConfiguredNit();

        String url = String.format("%sapi/Suministro/%s/%s", getBaseUrl(), nit, token);

        MipresSupplyRequest request = new MipresSupplyRequest(authorizationNumber, cupsCode, quantity, value);

        log.info("Reportando uso MIPRES: auth={}, cups={}, qty={}", authorizationNumber, cupsCode, quantity);

        try {
            MipresSupplyResponse response = webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MipresSupplyResponse.class)
                    .block();

            if (response == null || !response.isSuccess()) {
                String error = response != null ? response.errorMessage() : "Respuesta nula";
                log.warn("Error reportando uso MIPRES: {}", error);
                return new ConsumptionResult(false, error);
            }

            log.info("Uso MIPRES reportado exitosamente: supplyId={}", response.supplyId());
            return new ConsumptionResult(true, null);

        } catch (WebClientResponseException e) {
            log.error("Error HTTP reportando uso MIPRES: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new ConsumptionResult(false, "Error HTTP: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error reportando uso MIPRES", e);
            return new ConsumptionResult(false, e.getMessage());
        }
    }

    @Override
    public List<String> listarAutorizacionesVigentes(UUID contractId, UUID patientId) {
        String token = tokenService.getValidToken();
        String nit = getConfiguredNit();

        String url = String.format("%sapi/Prescripcion/%s/%s/%s",
                getBaseUrl(), nit, tokenService.getValidToken(), java.time.LocalDate.now());

        try {
            List<MipresPrescriptionListResponse> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (response == null) {
                return List.of();
            }

            return response.stream()
                    .map(MipresPrescriptionListResponse::prescriptionNumber)
                    .toList();

        } catch (Exception e) {
            log.error("Error listando autorizaciones MIPRES", e);
            return List.of();
        }
    }

    private String getBaseUrl() {
        return System.getenv().getOrDefault("MIPRES_BASE_URL", "https://wsmipres.sispro.gov.co/WSMIPRESNOPBS/");
    }

    private String getConfiguredNit() {
        String nit = System.getenv("MIPRES_NIT");
        if (nit == null || nit.isBlank()) {
            throw new IllegalStateException("MIPRES_NIT no configurado");
        }
        return nit;
    }

    // DTOs para respuestas de la API MIPRES
    record MipresPrescriptionResponse(
            String prescriptionNumber,
            String cupsCode,
            Integer qtyApproved,
            BigDecimal unitPriceCop,
            String diagnosisCie10,
            UUID contractId
    ) {}

    record MipresPrescriptionListResponse(
            String prescriptionNumber,
            String patientDocument,
            String patientName
    ) {}

    record MipresSupplyRequest(
            String authorizationNumber,
            String cupsCode,
            int quantity,
            BigDecimal value
    ) {}

    record MipresSupplyResponse(
            boolean success,
            String supplyId,
            String errorMessage
    ) {
        boolean isSuccess() {
            return success;
        }
    }

    public static class MipresClientException extends RuntimeException {
        public MipresClientException(String message) {
            super(message);
        }
    }
}