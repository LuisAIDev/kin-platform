package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.contract.TariffCups;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.AfRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfGenerator implements RipsGenerator {

    private final EpsContractRepository contractRepository;

    @Override
    public RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.AF;
    }

    @Override
    public List<RipsRecord> generate(RipsGenerationContext context) {
        EpsContract contract = contractRepository.findById(context.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato no encontrado: " + context.contractId()));

        BigDecimal valorTotal = calculateTotalFromOtherTypes(context);

        AfRecord af = AfRecord.fromContract(contract, context.periodStart(), context.periodEnd(), null, 0, valorTotal);
        RipsRecord r = new RipsRecord();
        r.setBatchId(null);
        r.setSequenceNumber(0);
        r.setSourceEntityType(af.getSourceEntityType());
        r.setSourceEntityId(af.getSourceEntityId());
        r.setRipsLineData(af.getRipsLineData());
        return List.of(r);
    }

    private BigDecimal calculateTotalFromOtherTypes(RipsGenerationContext context) {
        return context.tariffsByCups().values().stream()
                .findFirst()
                .map(TariffCups::getUnitPriceCop)
                .orElse(BigDecimal.ZERO);
    }
}