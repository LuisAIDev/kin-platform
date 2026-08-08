package com.kinplatform.project;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.reporting.report.ReportRepository;
import com.kinplatform.project.dto.ReportResponse;
import com.kinplatform.project.dto.ReportVersionResponse;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Expone el {@code ConsultingReport} estructurado de un proyecto al usuario
 * dueño del mismo.
 *
 * <ul>
 *   <li>{@code GET /projects/{projectId}/report}: última versión.</li>
 *   <li>{@code GET /projects/{projectId}/report?version=N}: versión concreta.</li>
 *   <li>{@code GET /projects/{projectId}/reports}: metadata de versiones.</li>
 * </ul>
 *
 * <p>Autorización: solo el dueño del proyecto (patrón de
 * {@link ProjectController}). Proyecto inexistente, ajeno o sin reporte se
 * responde 404 para no filtrar existencia.</p>
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectReportController {

    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @GetMapping("/{projectId}/report")
    public ResponseEntity<ReportResponse> getReport(
            Authentication auth,
            @PathVariable UUID projectId,
            @RequestParam(value = "version", required = false) Integer version) {
        var userId = getAuthenticatedUserId(auth);
        requireOwned(userId, projectId);
        var stored = version == null
                ? reportRepository.findLatest(projectId)
                : reportRepository.findByVersion(projectId, version);
        var found = stored.orElseThrow(() -> new ReportNotFoundException(
                version == null
                        ? "Aún no se ha generado el reporte de este proyecto"
                        : "Reporte no disponible"));
        return ResponseEntity.ok(toResponse(projectId, found));
    }

    @GetMapping("/{projectId}/reports")
    public ResponseEntity<List<ReportVersionResponse>> listReports(
            Authentication auth, @PathVariable UUID projectId) {
        var userId = getAuthenticatedUserId(auth);
        requireOwned(userId, projectId);
        var versions = reportRepository.listVersions(projectId).stream()
                .map(v -> new ReportVersionResponse(
                        v.version(), v.reportId(), v.reportVersion(), v.generatedAt()))
                .toList();
        return ResponseEntity.ok(versions);
    }

    private ReportResponse toResponse(UUID projectId, ReportRepository.StoredReport stored) {
        var report = stored.report();
        return new ReportResponse(
                projectId,
                stored.version(),
                report.id(),
                report.metadata() == null ? "" : report.metadata().reportVersion(),
                report.metadata() == null ? null : report.metadata().generatedAt(),
                report);
    }

    private Project requireOwned(UUID userId, UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ReportNotFoundException("Reporte no disponible"));
        if (!project.getUser().getId().equals(userId)) {
            throw new ReportNotFoundException("Reporte no disponible");
        }
        return project;
    }

    private UUID getAuthenticatedUserId(Authentication auth) {
        return AuthenticatedUsers.require(userRepository, auth).getId();
    }
}
