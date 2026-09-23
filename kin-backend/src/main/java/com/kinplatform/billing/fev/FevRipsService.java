package com.kinplatform.billing.fev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FevRipsService {

    private static final String[] VALUE_KEYS = {
            "valor_total", "valorConsulta", "valor_urgencia", "valorProcedimiento", "valor_unitario"
    };

    private final FevRipsInvoiceRepository invoiceRepository;
    private final RipsBatchRepository batchRepository;
    private final EpsContractRepository contractRepository;
    private final RipsRecordRepository recordRepository;
    private final XmlSigner xmlSigner;
    private final DianClient dianClient;
    private final ContingencyManager contingencyManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public FevRipsInvoice generateInvoiceFromBatch(UUID batchId) {
        UUID organizationId = TenantContext.get();

        RipsBatch batch = batchRepository.findById(batchId)
                .filter(b -> organizationId.equals(b.getOrganizationId()))
                .orElseThrow(() -> new EntityNotFoundException("Batch no encontrado: " + batchId));

        EpsContract contract = contractRepository.findByIdAndOrganizationId(batch.getContractId(), organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado: " + batch.getContractId()));

        long sequence = (contract.getDianCurrentSequence() == null ? 0L : contract.getDianCurrentSequence()) + 1;
        String prefix = contract.getDianPrefix() != null ? contract.getDianPrefix() : "FEV";
        int paymentTerms = contract.getPaymentTermsDays() != null ? contract.getPaymentTermsDays() : 30;

        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .organizationId(organizationId)
                .contractId(batch.getContractId())
                .batchId(batch.getId())
                .invoicePrefix(prefix)
                .invoiceSequence(sequence)
                .invoiceNumber(prefix + String.format("%010d", sequence))
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(paymentTerms))
                .totalValueCop(sumBatchTotal(batch.getId()))
                .status(FevRipsInvoice.InvoiceStatus.DRAFT)
                .build();

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public FevRipsInvoice sign(UUID id) {
        FevRipsInvoice invoice = load(id);
        String xml = buildUnsignedXml(invoice);
        XmlSigner.Signature signature = xmlSigner.sign(xml);
        invoice.markSigned(signature.cufe(), signature.qrCode());
        invoice.setSignedXmlPath("fev-rips/" + invoice.getId() + ".xml");
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public FevRipsInvoice sendToDian(UUID id) {
        FevRipsInvoice invoice = load(id);
        if (invoice.getStatus() != FevRipsInvoice.InvoiceStatus.SIGNED
                && invoice.getStatus() != FevRipsInvoice.InvoiceStatus.CONTINGENCY) {
            throw new IllegalStateException("La factura debe estar firmada antes de enviarse a la DIAN");
        }
        invoice.markSent();
        DianClient.DianResponse response = dianClient.send(invoice);
        if (response.accepted()) {
            invoice.markAccepted(response.responseXml());
        } else {
            invoice.markRejected(response.responseXml());
        }
        if (response.cufe() != null) {
            invoice.setDianCufe(response.cufe());
        }
        return invoiceRepository.save(invoice);
    }

    public FevRipsInvoice getStatus(UUID id) {
        return load(id);
    }

    @Transactional
    public FevRipsInvoice enterContingency(UUID id, String reason) {
        FevRipsInvoice invoice = load(id);
        return contingencyManager.enterContingency(invoice, reason);
    }

    private FevRipsInvoice load(UUID id) {
        return invoiceRepository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Factura FEV-RIPS no encontrada: " + id));
    }

    private BigDecimal sumBatchTotal(UUID batchId) {
        List<RipsRecord> records = recordRepository.findByBatchIdOrderBySequenceNumber(batchId);
        BigDecimal total = BigDecimal.ZERO;
        for (RipsRecord record : records) {
            BigDecimal value = extractValue(record.getRipsLineData());
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private BigDecimal extractValue(String ripsLineData) {
        if (ripsLineData == null || ripsLineData.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(ripsLineData);
            for (String key : VALUE_KEYS) {
                JsonNode value = node.get(key);
                if (value != null && value.isNumber()) {
                    return value.decimalValue();
                }
            }
        } catch (Exception e) {
            log.warn("Linea RIPS no parseable para total FEV: {}", e.getMessage());
        }
        return null;
    }

    private String buildUnsignedXml(FevRipsInvoice invoice) {
        return String.format(
                "<FEV-RIPS><NumeroFactura>%s</NumeroFactura><Prefijo>%s</Prefijo>"
                        + "<Secuencia>%d</Secuencia><FechaEmision>%s</FechaEmision>"
                        + "<ValorTotal>%s</ValorTotal><BatchId>%s</BatchId></FEV-RIPS>",
                invoice.getInvoiceNumber(), invoice.getInvoicePrefix(), invoice.getInvoiceSequence(),
                invoice.getIssueDate(), invoice.getTotalValueCop(), invoice.getBatchId());
    }
}
