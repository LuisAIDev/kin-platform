package com.kinplatform.billing.rips;

import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.common.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/billing/rips")
@RequiredArgsConstructor
@Slf4j
public class RipsController {

    private final RipsGenerationService ripsService;
    private final RipsBatchRepository batchRepository;

    /**
     * Genera RIPS para un contrato y período (los 6 tipos).
     * POST /api/v1/billing/rips/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<RipsBatchResponse> generate(
            @Valid @RequestBody GenerateRipsRequest req) {

        UUID orgId = TenantContext.get();
        log.info("Generando RIPS para contrato {} período {}-{}", 
            req.contractId(), req.periodStart(), req.periodEnd());

        RipsBatch batch = ripsService.generateAllTypes(orgId, req.contractId(), req.periodStart(), req.periodEnd());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RipsBatchResponse.from(batch));
    }

    /**
     * Consulta estado de un batch.
     * GET /api/v1/billing/rips/{batchId}
     */
    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<RipsBatchResponse> getBatch(@PathVariable UUID batchId) {
        UUID orgId = TenantContext.get();
        RipsBatch batch = batchRepository.findByIdAndOrganizationId(batchId, orgId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));
        return ResponseEntity.ok(RipsBatchResponse.from(batch));
    }

    /**
     * Lista batches por contrato con paginación.
     * GET /api/v1/billing/rips/by-contract/{contractId}
     */
    @GetMapping("/by-contract/{contractId}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<Page<RipsBatchResponse>> listByContract(
            @PathVariable UUID contractId,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID orgId = TenantContext.get();
        Page<RipsBatch> page = batchRepository.findByOrganizationIdAndContractId(orgId, contractId, pageable);
        return ResponseEntity.ok(page.map(RipsBatchResponse::from));
    }

    /**
     * Descarga el batch como ZIP.
     * GET /api/v1/billing/rips/{batchId}/download
     */
    @GetMapping("/{batchId}/download")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable UUID batchId) {
        UUID orgId = TenantContext.get();
        byte[] zip = ripsService.downloadAsZip(orgId, batchId);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"rips_" + batchId + ".zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(zip);
    }

    /**
     * Reenvía un batch fallido.
     * POST /api/v1/billing/rips/{batchId}/retry
     */
    @PostMapping("/{batchId}/retry")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<RipsBatchResponse> retry(@PathVariable UUID batchId) {
        UUID orgId = TenantContext.get();
        RipsBatch batch = ripsService.retry(orgId, batchId);
        return ResponseEntity.ok(RipsBatchResponse.from(batch));
    }

    @ExceptionHandler(BatchNotFoundException.class)
    public ResponseEntity<String> handleBatchNotFound(BatchNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}