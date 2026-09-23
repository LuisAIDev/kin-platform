package com.kinplatform.billing.contract;

import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TariffImportService {

    private final TariffCupsRepository tariffRepository;
    private final EpsContractRepository contractRepository;
    private final CupsExcelImporter excelImporter;

    @Transactional
    public TariffImportResult importTariffs(UUID contractId, MultipartFile file) throws IOException {
        contractRepository.findByIdAndOrganizationId(contractId, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + contractId));

        List<TariffCups> tariffs = excelImporter.parse(file);
        log.info("Parsed {} tariffs from file for contract {}", tariffs.size(), contractId);

        int created = 0, updated = 0, errors = 0;
        List<String> errorMessages = new ArrayList<>();

        for (TariffCups tariff : tariffs) {
            try {
                tariff.setContractId(contractId);
                Optional<TariffCups> existing = tariffRepository
                        .findByContractIdAndCupsCodeAndEffectiveFrom(contractId, tariff.getCupsCode(), tariff.getEffectiveFrom());

                if (existing.isPresent()) {
                    TariffCups toUpdate = existing.get();
                    toUpdate.setDescription(tariff.getDescription());
                    toUpdate.setUnitPriceCop(tariff.getUnitPriceCop());
                    toUpdate.setMaxQuantity(tariff.getMaxQuantity());
                    toUpdate.setRequiresAuth(tariff.getRequiresAuth());
                    toUpdate.setAuthValidityDays(tariff.getAuthValidityDays());
                    toUpdate.setCupsCategory(tariff.getCupsCategory());
                    toUpdate.setEffectiveTo(tariff.getEffectiveTo());
                    tariffRepository.save(toUpdate);
                    updated++;
                } else {
                    tariffRepository.save(tariff);
                    created++;
                }
            } catch (Exception e) {
                errors++;
                errorMessages.add("CUPS " + tariff.getCupsCode() + ": " + e.getMessage());
                log.warn("Error importing tariff {}: {}", tariff.getCupsCode(), e.getMessage());
            }
        }

        return new TariffImportResult(created, updated, errors, errorMessages);
    }
}