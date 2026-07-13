package com.kinplatform.project;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.project.dto.ProjectResponse;
import com.kinplatform.project.dto.UpdateProjectRequest;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            Authentication auth,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        var userId = getAuthenticatedUserId(auth);
        var response = projectService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> getAll(
            Authentication auth,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var userId = getAuthenticatedUserId(auth);
        var projects = projectService.getAllByUser(userId, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(
            Authentication auth,
            @PathVariable UUID id
    ) {
        var userId = getAuthenticatedUserId(auth);
        var response = projectService.getById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        var userId = getAuthenticatedUserId(auth);
        var response = projectService.update(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication auth,
            @PathVariable UUID id
    ) {
        var userId = getAuthenticatedUserId(auth);
        projectService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID getAuthenticatedUserId(Authentication auth) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        return user.getId();
    }
}
