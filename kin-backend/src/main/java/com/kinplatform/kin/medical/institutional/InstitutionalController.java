package com.kinplatform.kin.medical.institutional;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/institutional")
@RequiredArgsConstructor
public class InstitutionalController {

    private final InstitutionalKpisService institutionalKpisService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<InstitutionalKpisResponse> kpis() {
        return ResponseEntity.ok(institutionalKpisService.compute());
    }
}

