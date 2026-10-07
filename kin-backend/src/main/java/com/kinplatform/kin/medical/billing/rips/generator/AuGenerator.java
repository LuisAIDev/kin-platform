package com.kinplatform.kin.medical.billing.rips.generator;

import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.kin.medical.billing.contract.TariffCups;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import com.kinplatform.kin.medical.billing.rips.model.AuRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuGenerator implements RipsGenerator {

    private final EpsContractRepository contractRepository;

    @Override
    public RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.AU;
    }

    @Override
    public List<RipsRecord> generate(RipsGenerationContext context) {
        EpsContract contract = contractRepository.findById(context.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado: " + context.contractId()));

        // Filter encounters of type URGENCIA
        List<Object> urgencias = context.encounters().stream()
                .filter(e -> "URGENCIA".equals(getEncounterType(e)))
                .toList();

        log.info("Generando AU para {} urgencias", urgencias.size());

        return urgencias.stream()
                .map(encounter -> {
                    AuRecord au = createAuRecord(encounter, contract, context);
                    RipsRecord r = new RipsRecord();
                    r.setSourceEntityType(au.getSourceEntityType());
                    r.setSourceEntityId(au.getSourceEntityId());
                    r.setRipsLineData(au.getRipsLineData());
                    return r;
                })
                .toList();
    }

    private String getEncounterType(Object encounter) {
        try {
            return (String) encounter.getClass().getMethod("getEncounterType").invoke(encounter);
        } catch (Exception e) {
            return "URGENCIA";
        }
    }

    private AuRecord createAuRecord(Object encounter, EpsContract contract, RipsGenerationContext context) {
        String tipoDocumento = getField(encounter, "getPatientDocumentType", "CC");
        String numeroDocumento = getField(encounter, "getPatientDocumentNumber", "0000000000");
        String fechaUrgencia = getField(encounter, "getEncounterDate", java.time.LocalDate.now().toString());
        String motivoUrgencia = getField(encounter, "getUrgencyReason", "CONSULTA");
        String codigoDiagnosticoSalida = getField(encounter, "getDischargeDiagnosisCode", "Z00");
        String destinoUsuario = getField(encounter, "getDischargeDestination", "1");
        String estadoUsuario = getField(encounter, "getDischargeStatus", "1");
        String cupsCode = getField(encounter, "getCupsCode", "890201");

        TariffCups tariff = context.getTariff(cupsCode);
        BigDecimal valorUrgencia = tariff != null ? tariff.getUnitPriceCop() : BigDecimal.ZERO;

        return AuRecord.fromEncounter(
                encounter,
                contract,
                context.periodStart(),
                context.periodEnd(),
                getField(encounter, "getPatientDocumentType", "CC"),
                getField(encounter, "getPatientDocumentNumber", "0000000000"),
                fechaUrgencia,
                motivoUrgencia,
                codigoDiagnosticoSalida,
                destinoUsuario,
                estadoUsuario,
                cupsCode,
                valorUrgencia
        );
    }

    private String getField(Object obj, String methodName, String defaultValue) {
        try {
            return (String) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

