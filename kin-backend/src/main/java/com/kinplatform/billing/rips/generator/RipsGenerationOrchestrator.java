package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.rips.RipsValidationException;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import com.kinplatform.billing.rips.validator.ValidationResult;
import com.kinplatform.billing.rips.validator.XsdValidator;
import com.kinplatform.billing.rips.validator.BusinessRuleValidator;
import com.kinplatform.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RipsGenerationOrchestrator {

    private final List<RipsGenerator> generators;
    private final RipsBatchRepository batchRepository;
    private final RipsRecordRepository recordRepository;
    private final XsdValidator xsdValidator;
    private final BusinessRuleValidator businessValidator;
    private final RipsContextBuilder contextBuilder;

    public RipsBatch execute(UUID organizationId, UUID contractId, LocalDate periodStart, LocalDate periodEnd, RipsBatch.RipsType ripsType) {

        var existing = batchRepository.findByOrganizationIdAndContractIdAndPeriodStartAndPeriodEndAndRipsType(
                organizationId, contractId, periodStart, periodEnd, ripsType);
        if (existing.isPresent()) {
            RipsBatch batch = existing.get();
            if (batch.getStatus() == RipsBatch.BatchStatus.VALID
                    || batch.getStatus() == RipsBatch.BatchStatus.SENT_TO_DIAN
                    || batch.getStatus() == RipsBatch.BatchStatus.ACCEPTED) {
                log.info("Batch ya existe y es válido: {}", batch.getId());
                return batch;
            }
            batchRepository.delete(batch);
        }

        RipsBatch batch = RipsBatch.builder()
                .organizationId(organizationId)
                .contractId(contractId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .ripsType(ripsType)
                .status(RipsBatch.BatchStatus.GENERATING)
                .recordCount(0)
                .errorCount(0)
                .generatedAt(java.time.OffsetDateTime.now())
                .build();
        batch = batchRepository.save(batch);

        try {
            RipsGenerationContext context = contextBuilder.build(contractId, periodStart, periodEnd);

            for (RipsGenerator gen : generators) {
                if (!gen.supports(ripsType)) continue;

                List<RipsRecord> records = gen.generate(context);
                UUID batchId = batch.getId();
                records.forEach(r -> r.setRipsType(ripsType));
                records.forEach(r -> r.setBatchId(batchId));
                recordRepository.saveAll(records);
                batch.incrementRecordCount(records.size());
                log.debug("Generador {} produjo {} registros", gen.getClass().getSimpleName(), records.size());
            }

            batch = batchRepository.save(batch);

            batch.setStatus(RipsBatch.BatchStatus.VALIDATING);
            batch = batchRepository.save(batch);

            ValidationResult xsd = xsdValidator.validate(batch);
            ValidationResult biz = businessValidator.validate(batch, context);

            if (!xsd.isValid() || !biz.isValid()) {
                String errors = mergeErrors(xsd, biz);
                batch.markInvalid(errors);
                batch = batchRepository.save(batch);
                throw new RipsValidationException("Validación fallida: " + errors);
            }

            batch.markValid();
            batch = batchRepository.save(batch);

            log.info("RIPS {} generado exitosamente: {} registros", ripsType, batch.getRecordCount());
            return batch;

        } catch (Exception e) {
            batch.markError(e.getMessage());
            batchRepository.save(batch);
            throw e;
        }
    }

    public RipsBatch executeAllTypes(UUID organizationId, UUID contractId, LocalDate periodStart, LocalDate periodEnd) {
        for (RipsBatch.RipsType type : RipsBatch.RipsType.values()) {
            execute(organizationId, contractId, periodStart, periodEnd, type);
        }
        return batchRepository.findByOrganizationIdAndContractIdAndPeriodStartAndPeriodEndAndRipsType(
                organizationId, contractId, periodStart, periodEnd, RipsBatch.RipsType.AT)
                .orElseThrow();
    }

    private String mergeErrors(ValidationResult xsd, ValidationResult biz) {
        StringBuilder sb = new StringBuilder();
        if (!xsd.isValid()) sb.append("XSD: ").append(xsd.getErrors()).append("; ");
        if (!biz.isValid()) sb.append("Business: ").append(biz.getErrors());
        return sb.toString();
    }
}