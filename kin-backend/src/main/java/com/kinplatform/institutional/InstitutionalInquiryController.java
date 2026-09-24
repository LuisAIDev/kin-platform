package com.kinplatform.institutional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/institutional/inquiries")
@RequiredArgsConstructor
public class InstitutionalInquiryController {

    private static final String AUTO_REPLY_MESSAGE =
            "Hemos recibido tu solicitud. Te contactaremos en 48h para agendar una demo.";

    private final InstitutionalInquiryService service;

    @PostMapping
    public ResponseEntity<InstitutionalInquiryResponse> create(@Valid @RequestBody InstitutionalInquiryRequest request) {
        InstitutionalInquiry created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new InstitutionalInquiryResponse(created.getId(), created.getStatus().name(), AUTO_REPLY_MESSAGE));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InstitutionalInquiry>> list(
            @RequestParam(required = false) InstitutionalInquiry.InquiryStatus status) {
        return ResponseEntity.ok(status == null ? service.findAll() : service.findByStatus(status));
    }
}
