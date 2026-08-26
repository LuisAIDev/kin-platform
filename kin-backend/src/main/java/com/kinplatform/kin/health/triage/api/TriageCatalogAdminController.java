package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.triage.adapter.HealthDataImporter;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos del módulo de triaje (ADR-028, fase de
 * consolidación).
 *
 * <ul>
 *   <li>{@code POST /api/v1/admin/health/triage/catalog/update} — actualiza el
 *       catálogo desde las fuentes externas (KnowledgeEngine + bundle).</li>
 *   <li>{@code POST /api/v1/admin/health/catalog/import-from-external} — importa
 *       condiciones/síntomas desde fuentes externas (HealthDataImporter).</li>
 *   <li>{@code GET /api/v1/admin/health/catalog/coverage} — informe de cobertura
 *       de síntomas por condición (validación clínica).</li>
 * </ul>
 *
 * <p>Protegido por rol ADMIN (SecurityConfig: {@code /admin/**}). Las
 * importaciones son idempotentes y nunca rompen el catálogo local si la fuente
 * falla (degradación elegante).</p>
 */
@RestController
@RequestMapping("/admin/health")
public class TriageCatalogAdminController {

    private static final Logger log = LoggerFactory.getLogger(TriageCatalogAdminController.class);

    private final TriageCatalogUpdateService catalogUpdateService;
    private final HealthDataImporter healthDataImporter;
    private final CoverageReportService coverageReportService;

    public TriageCatalogAdminController(
            TriageCatalogUpdateService catalogUpdateService,
            HealthDataImporter healthDataImporter,
            CoverageReportService coverageReportService) {
        this.catalogUpdateService = catalogUpdateService;
        this.healthDataImporter = healthDataImporter;
        this.coverageReportService = coverageReportService;
    }

    @PostMapping("/triage/catalog/update")
    public ResponseEntity<CatalogUpdateResponse> updateCatalog() {
        log.info("=== TRIAGE CATALOG UPDATE (admin) ===");
        CatalogUpdateResult result = catalogUpdateService.updateCatalog();
        return ResponseEntity.ok(CatalogUpdateResponse.from(result));
    }

    @PostMapping("/catalog/import-from-external")
    public ResponseEntity<CatalogUpdateResponse> importFromExternal() {
        log.info("=== HEALTH CATALOG IMPORT FROM EXTERNAL (admin) ===");
        CatalogUpdateResult result = healthDataImporter.importFromExternal();
        return ResponseEntity.ok(CatalogUpdateResponse.from(result));
    }

    @GetMapping("/catalog/coverage")
    public ResponseEntity<CoverageReportService.CoverageReport> coverage() {
        log.info("=== HEALTH CATALOG COVERAGE (admin) ===");
        return ResponseEntity.ok(coverageReportService.generate());
    }
}
