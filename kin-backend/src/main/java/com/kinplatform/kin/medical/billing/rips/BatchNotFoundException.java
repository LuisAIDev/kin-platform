package com.kinplatform.kin.medical.billing.rips;

import java.util.UUID;

public class BatchNotFoundException extends RuntimeException {
    public BatchNotFoundException(UUID batchId) {
        super("Batch RIPS no encontrado: " + batchId);
    }
}
