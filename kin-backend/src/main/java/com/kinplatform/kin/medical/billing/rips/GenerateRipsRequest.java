package com.kinplatform.kin.medical.billing.rips;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GenerateRipsRequest(
    @NotNull UUID contractId,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd
) {
}
