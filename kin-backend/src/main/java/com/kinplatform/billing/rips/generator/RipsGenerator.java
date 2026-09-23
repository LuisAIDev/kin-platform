package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import java.util.List;

public interface RipsGenerator {

    List<RipsRecord> generate(RipsGenerationContext context);

    RipsBatch.RipsType getType();

    default boolean supports(RipsBatch.RipsType type) {
        return type == getType();
    }
}