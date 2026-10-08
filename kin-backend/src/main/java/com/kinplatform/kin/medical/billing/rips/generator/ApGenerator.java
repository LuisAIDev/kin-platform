package com.kinplatform.kin.medical.billing.rips.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.medical.billing.authorization.AuthorizationRepository;
import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.kin.medical.billing.contract.TariffCups;
import com.kinplatform.kin.medical.billing.rips.RipsSerializationException;
import com.kinplatform.kin.medical.billing.rips.model.ApRecord;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApGenerator implements RipsGenerator {

    private final EpsContractRepository contractRepository;
    private final AuthorizationRepository authRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.AP;
    }

    @Override
    public List<RipsRecord> generate(RipsGenerationContext context) {
        EpsContract contract = contractRepository
                .findById(context.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado: " + context.contractId()));

        List<Object> procedimientos = context.orders().stream()
                .filter(o -> "PROCEDIMIENTO".equals(getOrderType(o)))
                .toList();

        log.info("Generando AP para {} procedimientos", procedimientos.size());

        return procedimientos.stream()
                .map(order -> {
                    ApRecord ap = createApRecord(order, contract, context);
                    RipsRecord r = new RipsRecord();
                    r.setSourceEntityType("ORDER");
                    r.setSourceEntityId(getOrderId(order));
                    r.setRipsLineData(buildApJsonLine(order, ap));
                    return r;
                })
                .toList();
    }

    private String getOrderType(Object order) {
        try {
            return (String) order.getClass().getMethod("getOrderType").invoke(order);
        } catch (Exception e) {
            return "PROCEDIMIENTO";
        }
    }

    private UUID getOrderId(Object order) {
        try {
            return (UUID) order.getClass().getMethod("getId").invoke(order);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }

    private ApRecord createApRecord(Object order, EpsContract contract, RipsGenerationContext context) {
        String cupsCode = getField(order, "getCupsCode", "890301");
        String fechaProcedimiento =
                getField(order, "getOrderDate", java.time.LocalDate.now().toString());
        String codigoDiagnostico = getField(order, "getDiagnosisCode", "Z00");
        String ambitoRealizacion = getField(order, "getAmbitoRealizacion", "1");
        String finalidadProcedimiento = getField(order, "getFinalidadProcedimiento", "1");

        String authorizationNumber = getField(order, "getAuthorizationNumber", "");
        // TODO: Validate and consume authorization

        TariffCups tariff = context.getTariff(cupsCode);
        BigDecimal valorProcedimiento = tariff != null ? tariff.getUnitPriceCop() : BigDecimal.ZERO;

        return ApRecord.builder()
                .sourceEntityType("ORDER")
                .sourceEntityId(getOrderId(order))
                .codigoPrestador(contract.getOrganizationId().toString().substring(0, 10))
                .numFactura(contract.getDianPrefix() + String.format("%010d", contract.getDianCurrentSequence() + 1))
                .codigoProcedimiento(cupsCode)
                .fechaProcedimiento(java.time.LocalDate.parse(getField(
                        order, "getOrderDate", java.time.LocalDate.now().toString())))
                .codigoDiagnostico(getField(order, "getDiagnosisCode", "Z00"))
                .valorProcedimiento(tariff != null ? tariff.getUnitPriceCop() : BigDecimal.ZERO)
                .ambitoRealizacion(getField(order, "getAmbitoRealizacion", "1"))
                .finalidadProcedimiento(getField(order, "getFinalidadProcedimiento", "1"))
                .build();
    }

    private String getField(Object obj, String methodName, String defaultValue) {
        try {
            return (String) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String buildApJsonLine(Object order, ApRecord ap) {
        try {
            return objectMapper.writeValueAsString(ap);
        } catch (JsonProcessingException e) {
            log.error("Error CRITICO serializando AP a JSON: {}", e.getMessage(), e);
            throw new RipsSerializationException("No se pudo serializar AP: " + e.getMessage(), e);
        }
    }
}
