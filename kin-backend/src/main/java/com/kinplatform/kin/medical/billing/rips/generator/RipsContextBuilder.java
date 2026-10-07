package com.kinplatform.kin.medical.billing.rips.generator;

import com.kinplatform.kin.medical.billing.contract.TariffCups;
import com.kinplatform.kin.medical.billing.authorization.Authorization;
import com.kinplatform.kin.medical.billing.contract.TariffCupsRepository;
import com.kinplatform.kin.medical.billing.authorization.AuthorizationRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RipsContextBuilder {

    private final UserRepository userRepository;
    private final TariffCupsRepository tariffRepository;
    private final AuthorizationRepository authRepository;

    public RipsGenerationContext build(UUID contractId, LocalDate periodStart, LocalDate periodEnd) {
        List<Object> encounters = List.of();
        List<Object> orders = List.of();
        List<User> patients = List.of();
        List<User> practitioners = List.of();

        Map<String, TariffCups> tariffsByCups = tariffRepository.findValidTariffsForDate(contractId, periodEnd)
                .stream()
                .collect(Collectors.toMap(TariffCups::getCupsCode, t -> t, (a, b) -> a));

        Map<UUID, Authorization> authsById = authRepository.findByContractIdAndStatus(
                contractId, Authorization.AuthorizationStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(Authorization::getId, a -> a, (a, b) -> a));

        return new RipsGenerationContext(
                null,
                contractId,
                periodStart,
                periodEnd,
                encounters,
                orders,
                patients,
                practitioners,
                tariffsByCups,
                authsById
        );
    }
}

