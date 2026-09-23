package com.kinplatform.billing.fev;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/billing/fev-rips")
@RequiredArgsConstructor
public class FevRipsController {

    private final FevRipsService fevRipsService;

    @PostMapping("/generate/{batchId}")
    public ResponseEntity<FevRipsInvoice> generate(@PathVariable UUID batchId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fevRipsService.generateInvoiceFromBatch(batchId));
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<FevRipsInvoice> sign(@PathVariable UUID id) {
        return ResponseEntity.ok(fevRipsService.sign(id));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<FevRipsInvoice> send(@PathVariable UUID id) {
        return ResponseEntity.ok(fevRipsService.sendToDian(id));
    }

    @PostMapping("/{id}/contingency")
    public ResponseEntity<FevRipsInvoice> contingency(@PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(fevRipsService.enterContingency(id, reason));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<FevRipsInvoice> status(@PathVariable UUID id) {
        return ResponseEntity.ok(fevRipsService.getStatus(id));
    }
}
