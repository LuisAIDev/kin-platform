package com.kinplatform.platform.projectinfo.web;

import com.kinplatform.platform.projectinfo.ProjectStructuredInfoService;
import com.kinplatform.platform.projectinfo.dto.ConfirmInfoRequest;
import com.kinplatform.platform.projectinfo.dto.StructuredInfoRequest;
import com.kinplatform.platform.projectinfo.dto.StructuredInfoResponse;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de información estructurada del proyecto: {@code POST/GET
 * /projects/{projectId}/info} y confirmación de datos importados
 * ({@code POST /projects/{projectId}/info/{section}/{key}/confirm}). El
 * ownership se valida en el servicio.
 */
@RestController
@RequestMapping("/projects/{projectId}/info")
@RequiredArgsConstructor
public class ProjectStructuredInfoController {

    private final ProjectStructuredInfoService infoService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<List<StructuredInfoResponse>> upsert(
            Authentication auth, @PathVariable UUID projectId, @Valid @RequestBody StructuredInfoRequest request) {
        UUID userId = getAuthenticatedUserId(auth);
        return ResponseEntity.ok(infoService.upsert(userId, projectId, request.getEntries()));
    }

    @GetMapping
    public ResponseEntity<List<StructuredInfoResponse>> list(Authentication auth, @PathVariable UUID projectId) {
        UUID userId = getAuthenticatedUserId(auth);
        return ResponseEntity.ok(infoService.listByProject(userId, projectId));
    }

    @PostMapping("/{section}/{key}/confirm")
    public ResponseEntity<StructuredInfoResponse> confirm(
            Authentication auth,
            @PathVariable UUID projectId,
            @PathVariable String section,
            @PathVariable String key,
            @Valid @RequestBody(required = false) ConfirmInfoRequest request) {
        UUID userId = getAuthenticatedUserId(auth);
        String sourceDocument = request != null ? request.getSourceDocument() : null;
        return ResponseEntity.ok(infoService.confirm(userId, projectId, section, key, sourceDocument));
    }

    private UUID getAuthenticatedUserId(Authentication auth) {
        return com.kinplatform.common.security.AuthenticatedUsers.require(userRepository, auth)
                .getId();
    }
}


