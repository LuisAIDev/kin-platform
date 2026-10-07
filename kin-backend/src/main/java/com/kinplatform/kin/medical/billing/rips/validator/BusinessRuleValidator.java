package com.kinplatform.kin.medical.billing.rips.validator;

import com.kinplatform.kin.medical.billing.rips.generator.RipsGenerationContext;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BusinessRuleValidator {

    private final RipsRecordRepository recordRepository;

    public ValidationResult validate(RipsBatch batch, RipsGenerationContext context) {
        List<String> errors = new ArrayList<>();

        if (batch.getRipsType() == RipsBatch.RipsType.AF) {
            BigDecimal totalCalculated = calculateTotalFromDetails(context, batch);
            BigDecimal afTotal = extractAfTotal(batch);

            if (afTotal != null && totalCalculated != null
                    && afTotal.subtract(totalCalculated).abs().compareTo(new BigDecimal("0.01")) > 0) {
                errors.add("AF valor_total (" + afTotal + ") no coincide con suma AC+AP+AU+AT (" + totalCalculated + ")");
            }
        }

        validateAuthorizations(batch, context, errors);
        validateCupsInTariff(batch, context, errors);
        validateDiagnosisCodes(batch, errors);

        if (!errors.isEmpty()) {
            return ValidationResult.invalid(errors);
        }
        return ValidationResult.valid();
    }

    private BigDecimal calculateTotalFromDetails(RipsGenerationContext context, RipsBatch afBatch) {
        return BigDecimal.ZERO;
    }

    private BigDecimal extractAfTotal(RipsBatch batch) {
        return BigDecimal.ZERO;
    }

    private void validateAuthorizations(RipsBatch batch, RipsGenerationContext context, List<String> errors) {
    }

    private void validateCupsInTariff(RipsBatch batch, RipsGenerationContext context, List<String> errors) {
    }

    private void validateDiagnosisCodes(RipsBatch batch, List<String> errors) {
    }
}

