package com.kinplatform.kin.medical.billing.contract;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/billing/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<Page<EpsContract>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contractService.findAll(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<List<EpsContract>> listActiveForBilling() {
        return ResponseEntity.ok(contractService.findActiveForBilling());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<EpsContract> get(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @GetMapping("/validate-nit/{nit}")
    @PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<Void> validateNit(@PathVariable String nit) {
        boolean exists = contractService.contractRepository().existsByOrganizationIdAndEpsNit(
                com.kinplatform.common.security.TenantContext.get(), nit.trim().toUpperCase());
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/tariffs/stats")
    @PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<TariffStatsResponse> getTariffStats(@PathVariable UUID id) {
        contractService.findById(id);
        long count = contractService.tariffRepository().countByContractId(id);
        return ResponseEntity.ok(new TariffStatsResponse(count));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<EpsContract> create(@Valid @RequestBody ContractService.CreateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<EpsContract> update(@PathVariable UUID id, @Valid @RequestBody ContractService.UpdateContractRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{contractId}/tariffs/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<TariffImportResult> importTariffs(
            @PathVariable UUID contractId,
            @RequestPart("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                    .body(new TariffImportResult(0, 0, 1, List.of("Only .xlsx files allowed")));
        }

        TariffImportResult result = contractService.importTariffs(contractId, file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{contractId}/tariffs/template")
    @PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = generateTemplate();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=cups_tariffs_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }

    private byte[] generateTemplate() {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = workbook.createSheet("Tarifarios CUPS");
            var header = sheet.createRow(0);
            String[] headers = {"CUPS_CODE", "DESCRIPTION", "UNIT_PRICE_COPS", "CUPS_CATEGORY", "REQUIRES_AUTH", "AUTH_VALIDITY_DAYS", "EFFECTIVE_FROM", "EFFECTIVE_TO"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            var bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating template", e);
        }
    }
}

