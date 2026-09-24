package com.kinplatform.institutional;

import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/institutional/members")
@RequiredArgsConstructor
public class OrganizationMemberController {

    private final OrganizationMemberService service;

    @GetMapping
    public ResponseEntity<List<OrganizationMember>> list(@RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(branchId == null ? service.findAll() : service.findByBranch(branchId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<OrganizationMember> invite(@Valid @RequestBody OrganizationMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(request));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrganizationMember> accept(@PathVariable UUID id, HttpServletRequest request) {
        User current = (User) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE);
        OrganizationMember member = service.get(id);
        if (current == null || !member.getUserId().equals(current.getId())) {
            throw new AccessDeniedException("No puedes aceptar una invitacion ajena");
        }
        return ResponseEntity.ok(service.accept(id));
    }

    @PutMapping("/{id}/branch/{branchId}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<OrganizationMember> assignBranch(@PathVariable UUID id, @PathVariable UUID branchId) {
        return ResponseEntity.ok(service.assignBranch(id, branchId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'ADMIN')")
    public ResponseEntity<OrganizationMember> remove(@PathVariable UUID id) {
        return ResponseEntity.ok(service.remove(id));
    }
}
