package com.kinplatform.billing.contract;

import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final EpsContractRepository contractRepository;
    private final TariffCupsRepository tariffRepository;
    private final CopayRuleRepository copayRepository;
    private final ContractValidator validator;
    private final TariffImportService importService;

    public Page<EpsContract> findAll(Pageable pageable) {
        return contractRepository.findByOrganizationId(TenantContext.get(), pageable);
    }

    public List<EpsContract> findActiveForBilling() {
        return contractRepository.findActiveContractsForBilling(TenantContext.get());
    }

    public EpsContract findById(UUID id) {
        return contractRepository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found: " + id));
    }

    @Transactional
    public EpsContract create(CreateContractRequest request) {
        validator.validateCreate(request);

        EpsContract contract = EpsContract.builder()
                .organizationId(TenantContext.get())
                .epsNit(request.epsNit().trim().toUpperCase())
                .epsName(request.epsName().trim())
                .regimen(EpsContract.Regimen.valueOf(request.regimen()))
                .contractNumber(request.contractNumber())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(EpsContract.ContractStatus.ACTIVE)
                .billingCycle(EpsContract.BillingCycle.valueOf(request.billingCycle()))
                .paymentTermsDays(request.paymentTermsDays())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .dianPrefix(request.dianPrefix())
                .dianResolutionNumber(request.dianResolutionNumber())
                .dianCurrentSequence(0L)
                .build();

        return contractRepository.save(contract);
    }

    @Transactional
    public EpsContract update(UUID id, UpdateContractRequest request) {
        EpsContract contract = findById(id);
        if (request.epsName() != null) contract.setEpsName(request.epsName().trim());
        if (request.contractNumber() != null) contract.setContractNumber(request.contractNumber());
        if (request.startDate() != null) contract.setStartDate(request.startDate());
        if (request.endDate() != null) contract.setEndDate(request.endDate());
        if (request.status() != null) contract.setStatus(EpsContract.ContractStatus.valueOf(request.status()));
        if (request.billingCycle() != null) contract.setBillingCycle(EpsContract.BillingCycle.valueOf(request.billingCycle()));
        if (request.paymentTermsDays() != null) contract.setPaymentTermsDays(request.paymentTermsDays());
        if (request.contactEmail() != null) contract.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) contract.setContactPhone(request.contactPhone());
        if (request.dianPrefix() != null) contract.setDianPrefix(request.dianPrefix());
        if (request.dianResolutionNumber() != null) contract.setDianResolutionNumber(request.dianResolutionNumber());

        return contractRepository.save(contract);
    }

    @Transactional
    public void delete(UUID id) {
        EpsContract contract = findById(id);
        if (tariffRepository.countByContractId(id) > 0) {
            throw new IllegalStateException("Cannot delete contract with existing tariffs");
        }
        contractRepository.delete(contract);
    }

    public TariffImportResult importTariffs(UUID contractId, MultipartFile file) throws IOException {
        findById(contractId); // valida tenant
        return importService.importTariffs(contractId, file);
    }

    public EpsContractRepository contractRepository() {
        return contractRepository;
    }

    public TariffCupsRepository tariffRepository() {
        return tariffRepository;
    }

    public record CreateContractRequest(
            String epsNit,
            String epsName,
            String regimen,
            String contractNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String billingCycle,
            Integer paymentTermsDays,
            String contactEmail,
            String contactPhone,
            String dianPrefix,
            String dianResolutionNumber
    ) {}

    public record UpdateContractRequest(
            String epsName,
            String contractNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            String billingCycle,
            Integer paymentTermsDays,
            String contactEmail,
            String contactPhone,
            String dianPrefix,
            String dianResolutionNumber
    ) {}
}