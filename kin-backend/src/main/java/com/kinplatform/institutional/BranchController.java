package com.kinplatform.institutional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/institutional/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService service;

    @GetMapping
    public ResponseEntity<List<Branch>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Branch> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<Branch> create(@Valid @RequestBody BranchService.BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<Branch> update(@PathVariable UUID id,
                                         @Valid @RequestBody BranchService.BranchRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
