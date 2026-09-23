package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.contract.TariffCups;
import com.kinplatform.billing.authorization.Authorization;
import com.kinplatform.user.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RipsGenerationContext(
    UUID organizationId,
    UUID contractId,
    LocalDate periodStart,
    LocalDate periodEnd,
    List<Object> encounters,
    List<Object> orders,
    List<User> patients,
    List<User> practitioners,
    Map<String, TariffCups> tariffsByCups,
    Map<UUID, Authorization> authsById
) {
    public TariffCups getTariff(String cupsCode) {
        return tariffsByCups.get(cupsCode);
    }

    public Authorization getAuthorization(UUID authId) {
        return authsById.get(authId);
    }
}