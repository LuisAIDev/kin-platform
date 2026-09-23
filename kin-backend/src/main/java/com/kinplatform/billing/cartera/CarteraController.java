package com.kinplatform.billing.cartera;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/cartera")
@RequiredArgsConstructor
public class CarteraController {

    private final CarteraService carteraService;

    @PostMapping("/from-invoice/{fevInvoiceId}")
    public ResponseEntity<AccountsReceivable> registerInvoice(@PathVariable UUID fevInvoiceId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carteraService.registerInvoice(fevInvoiceId));
    }

    @GetMapping
    public ResponseEntity<List<AccountsReceivable>> list(
            @RequestParam(required = false) AccountsReceivable.ArStatus status) {
        return ResponseEntity.ok(status == null ? carteraService.findAll() : carteraService.findByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountsReceivable> get(@PathVariable UUID id) {
        return ResponseEntity.ok(carteraService.get(id));
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<AccountsReceivable> registerPayment(@PathVariable UUID id,
                                                             @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(carteraService.registerPayment(id, request.amount(), request.paymentDate()));
    }

    @GetMapping("/{id}/next-action")
    public ResponseEntity<CollectionWorkflow.CollectionAction> nextAction(@PathVariable UUID id) {
        return ResponseEntity.ok(carteraService.nextCollectionAction(id));
    }

    @GetMapping("/summary")
    public ResponseEntity<CarteraSummaryResponse> summary() {
        return ResponseEntity.ok(carteraService.summary());
    }

    public record PaymentRequest(BigDecimal amount, LocalDate paymentDate) {
    }
}
