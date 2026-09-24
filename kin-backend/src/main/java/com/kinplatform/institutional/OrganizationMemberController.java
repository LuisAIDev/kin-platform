package com.kinplatform.institutional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<OrganizationMember> invite(@Valid @RequestBody OrganizationMemberService.InviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(request));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<OrganizationMember> accept(@PathVariable UUID id) {
        return ResponseEntity.ok(service.accept(id));
    }

    @PutMapping("/{id}/branch/{branchId}")
    public ResponseEntity<OrganizationMember> assignBranch(@PathVariable UUID id, @PathVariable UUID branchId) {
        return ResponseEntity.ok(service.assignBranch(id, branchId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrganizationMember> remove(@PathVariable UUID id) {
        return ResponseEntity.ok(service.remove(id));
    }
}
