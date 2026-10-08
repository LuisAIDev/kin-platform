package com.kinplatform.kin.medical.billing.authorization;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing/authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<Page<Authorization>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(authorizationService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Authorization> get(@PathVariable UUID id) {
        return ResponseEntity.ok(authorizationService.findById(id));
    }

    @GetMapping("/by-contract/{contractId}")
    public ResponseEntity<List<Authorization>> findByContract(@PathVariable UUID contractId) {
        return ResponseEntity.ok(authorizationService.findByContract(contractId));
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<Authorization>> findExpiringSoon(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(authorizationService.findExpiringSoon(Duration.ofDays(days)));
    }

    @PostMapping
    public ResponseEntity<Authorization> create(
            @Valid @RequestBody AuthorizationService.CreateAuthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorizationService.create(request));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Authorization> approve(
            @PathVariable UUID id, @Valid @RequestBody AuthorizationService.ApproveAuthorizationRequest request) {
        return ResponseEntity.ok(authorizationService.approve(id, request));
    }

    @PostMapping("/{id}/consume")
    public ResponseEntity<Authorization> consume(
            @PathVariable UUID id,
            @RequestParam String cupsCode,
            @RequestParam int quantity,
            @RequestParam BigDecimal unitValue) {
        return ResponseEntity.ok(authorizationService.consume(id, cupsCode, quantity, unitValue));
    }

    @GetMapping("/valid-for-encounter")
    public ResponseEntity<Boolean> validateForEncounter(
            @RequestParam UUID patientId, @RequestParam String cupsCode, @RequestParam UUID contractId) {
        return ResponseEntity.ok(authorizationService.validateForEncounter(patientId, cupsCode, contractId));
    }
}
