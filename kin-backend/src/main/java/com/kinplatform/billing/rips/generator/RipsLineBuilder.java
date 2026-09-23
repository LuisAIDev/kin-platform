package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsBatch;

public interface RipsLineBuilder<T extends RipsRecord> {

    T build(RipsGenerationContext context, Object sourceEntity);

    default RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.US;
    }

    default String buildPatientDocument(Object source) {
        return "";
    }

    default String buildCupsCode(Object source) {
        return "";
    }

    default String buildDiagnosisCode(Object source, int position) {
        return "";
    }

    default java.math.BigDecimal buildValue(Object source) {
        return java.math.BigDecimal.ZERO;
    }
}