package com.kinplatform.kin.medical.admission.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import com.kinplatform.kin.medical.admission.domain.Patient;
import com.kinplatform.kin.medical.admission.service.AdmissionService;
import com.kinplatform.kin.medical.admission.service.PatientAlreadyExistsException;
import com.kinplatform.kin.medical.admission.service.PatientNotFoundException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medical/admission/patients")
public class AdmissionController {

    private static final Logger log = LoggerFactory.getLogger(AdmissionController.class);

    private final AdmissionService admissionService;
    private final UserRepository userRepository;

    public AdmissionController(AdmissionService admissionService, UserRepository userRepository) {
        this.admissionService = admissionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHYSICIAN', 'RECEPTIONIST', 'IPS_ADMIN', 'IPS_ENFERMERA', 'IPS_FACTURADOR')")
    public ResponseEntity<PatientResponse> createPatient(
            Authentication authentication, @Valid @RequestBody PatientDTO request) {
        User user = requireAuthorizedUser(authentication);
        PatientResponse response = admissionService.register(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHYSICIAN', 'RECEPTIONIST', 'IPS_ADMIN', 'IPS_ENFERMERA', 'IPS_FACTURADOR', 'PATIENT')")
    public ResponseEntity<PatientResponse> getPatient(
            Authentication authentication, @PathVariable UUID id) {
        User user = requireAuthorizedUser(authentication);
        // PATIENT role can only see their own record
        if (user.getRole() == UserRole.PATIENT && !user.getId().equals(id)) {
            throw new AccessDeniedException("Solo puede consultar su propio registro");
        }
        return admissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHYSICIAN', 'RECEPTIONIST', 'IPS_ADMIN', 'IPS_ENFERMERA', 'IPS_FACTURADOR')")
    public ResponseEntity<Page<PatientResponse>> searchPatients(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        requireAuthorizedUser(authentication);
        String query = q != null ? q.trim() : "";
        if (query.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }
        Page<PatientResponse> page = admissionService.search(query, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHYSICIAN', 'RECEPTIONIST', 'IPS_ADMIN', 'IPS_ENFERMERA', 'IPS_FACTURADOR')")
    public ResponseEntity<PatientResponse> getPatientByDocument(
            Authentication authentication,
            @RequestParam Patient.DocumentType documentType,
            @RequestParam String documentNumber) {
        requireAuthorizedUser(authentication);
        return admissionService.findByDocument(documentType, documentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHYSICIAN', 'RECEPTIONIST', 'IPS_ADMIN', 'IPS_ENFERMERA')")
    public ResponseEntity<PatientResponse> updatePatient(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody PatientDTO request) {
        User user = requireAuthorizedUser(authentication);
        PatientResponse response = admissionService.update(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'IPS_ADMIN')")
    public ResponseEntity<Void> deletePatient(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        User user = requireAuthorizedUser(authentication);
        admissionService.softDelete(id, user.getId(), reason);
        return ResponseEntity.noContent().build();
    }

    private User requireAuthorizedUser(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (user.getRole() == UserRole.FREE || user.getRole() == UserRole.PREMIUM || user.getRole() == UserRole.FACILITADOR) {
            throw new AccessDeniedException("Rol no autorizado para operaciones de admisión");
        }
        return user;
    }
}