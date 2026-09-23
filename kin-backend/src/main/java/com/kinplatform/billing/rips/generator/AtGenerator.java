package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.contract.TariffCups;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.AtRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AtGenerator implements RipsGenerator {

    private final EpsContractRepository contractRepository;

    @Override
    public RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.AT;
    }

    @Override
    public List<RipsRecord> generate(RipsGenerationContext context) {
        EpsContract contract = contractRepository.findById(context.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado: " + context.contractId()));

        // Filter orders of type MEDICAMENTO, INSUMO, DISPOSITIVO
        List<Object> otrosServicios = context.orders().stream()
                .filter(o -> {
                    String type = getOrderType(o);
                    return "MEDICAMENTO".equals(type) || "INSUMO".equals(type) || "DISPOSITIVO".equals(type);
                })
                .toList();

        log.info("Generando AT para {} otros servicios", otrosServicios.size());

        return otrosServicios.stream()
                .map(order -> {
                    AtRecord at = createAtRecord(order, contract, context);
                    RipsRecord r = new RipsRecord();
                    r.setSourceEntityType(at.getSourceEntityType());
                    r.setSourceEntityId(at.getSourceEntityId());
                    r.setRipsLineData(at.getRipsLineData());
                    return r;
                })
                .toList();
    }

    private String getOrderType(Object order) {
        try {
            return (String) order.getClass().getMethod("getOrderType").invoke(order);
        } catch (Exception e) {
            return "MEDICAMENTO";
        }
    }

    private AtRecord createAtRecord(Object order, EpsContract contract, RipsGenerationContext context) {
        String cupsCode = getField(order, "getCupsCode", "890501");
        String fechaServicio = getField(order, "getOrderDate", java.time.LocalDate.now().toString());
        String descripcion = getField(order, "getDescription", "Servicio");
        int cantidad = getFieldInt(order, "getQuantity", 1);
        String tipoOtrosServicios = getField(order, "getServiceType", "MEDICAMENTO");

        TariffCups tariff = context.getTariff(cupsCode);
        BigDecimal valorUnitario = context.getTariff(cupsCode) != null ? context.getTariff(cupsCode).getUnitPriceCop() : BigDecimal.ZERO;
        BigDecimal valorTotal = valorUnitario.multiply(BigDecimal.valueOf(cantidad));

        return AtRecord.fromOrder(
                order,
                contract,
                context.periodStart(),
                context.periodEnd(),
                getField(order, "getPatientDocumentType", "CC"),
                getField(order, "getPatientDocumentNumber", "0000000000"),
                cupsCode,
                descripcion,
                cantidad,
                valorUnitario,
                valorTotal,
                fechaServicio,
                tipoOtrosServicios
        );
    }

    private String getField(Object obj, String methodName, String defaultValue) {
        try {
            return (String) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int getFieldInt(Object obj, String methodName, int defaultValue) {
        try {
            return (int) obj.getClass().getMethod(methodName).invoke(obj);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}