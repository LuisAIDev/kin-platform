package com.kinplatform.billing.rips.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.contract.TariffCups;
import com.kinplatform.billing.rips.RipsSerializationException;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.AcRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AcGenerator implements RipsGenerator {

    private final EpsContractRepository contractRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.AC;
    }

    @Override
    public List<RipsRecord> generate(RipsGenerationContext context) {
        EpsContract contract = contractRepository.findById(context.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado: " + context.contractId()));

        List<Object> consultas = context.encounters().stream()
                .filter(e -> "CONSULTA".equals(getEncounterType(e)))
                .toList();

        log.info("Generando AC para {} consultas", consultas.size());

        return consultas.stream()
                .map(encounter -> {
                    AcRecord ac = createAcRecord(encounter, contract, context);
                    RipsRecord r = new RipsRecord();
                    r.setSourceEntityType("ENCOUNTER");
                    r.setSourceEntityId(getEncounterId(encounter));
                    r.setRipsLineData(buildAcJsonLine(encounter, contract, context, ac));
                    return r;
                })
                .toList();
    }

    private String getEncounterType(Object encounter) {
        try {
            return (String) encounter.getClass().getMethod("getEncounterType").invoke(encounter);
        } catch (Exception e) {
            return "CONSULTA";
        }
    }

    private UUID getEncounterId(Object encounter) {
        try {
            return (UUID) encounter.getClass().getMethod("getId").invoke(encounter);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }

    private AcRecord createAcRecord(Object encounter, EpsContract contract, RipsGenerationContext context) {
        String cupsCode = getField(encounter, "getCupsCode", "890201");
        String fechaConsulta = getField(encounter, "getEncounterDate", java.time.LocalDate.now().toString());
        String codigoDiagnosticoPrincipal = getField(encounter, "getDiagnosisCode", "Z00");
        String codigoDiagnosticoRelacionado = getField(encounter, "getRelatedDiagnosisCode", "");

        TariffCups tariff = context.getTariff(cupsCode);
        BigDecimal valorConsulta = tariff != null ? tariff.getUnitPriceCop() : BigDecimal.ZERO;

        return AcRecord.builder()
                .sourceEntityType("ENCOUNTER")
                .sourceEntityId(getEncounterId(encounter))
                .tipoDocumento(getField(encounter, "getPatientDocumentType", "CC"))
                .numDocumento(getField(encounter, "getPatientDocumentNumber", "0000000000"))
                .fechaConsulta(java.time.LocalDate.parse(fechaConsulta))
                .codigoDiagnosticoPrincipal(codigoDiagnosticoPrincipal)
                .codigoDiagnosticoRelacionado(codigoDiagnosticoRelacionado)
                .codigoConsulta(cupsCode)
                .valorConsulta(valorConsulta)
                .cuotaModeradora(BigDecimal.ZERO)
                .copago(BigDecimal.ZERO)
                .build();
    }

    private String getField(Object obj, String methodName, String defaultValue) {
        try {
            return (String) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String buildAcJsonLine(Object encounter, EpsContract contract, RipsGenerationContext context, AcRecord ac) {
        try {
            return objectMapper.writeValueAsString(ac);
        } catch (JsonProcessingException e) {
            log.error("Error CRITICO serializando AC a JSON: {}", e.getMessage(), e);
            throw new RipsSerializationException("No se pudo serializar AC: " + e.getMessage(), e);
        }
    }
}