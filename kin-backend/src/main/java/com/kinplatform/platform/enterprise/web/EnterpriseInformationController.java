package com.kinplatform.platform.enterprise.web;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.platform.enterprise.integration.DocumentDescriptor;
import com.kinplatform.platform.enterprise.integration.EnterpriseDataProvider;
import com.kinplatform.platform.enterprise.integration.EnterpriseIntegrationData;
import com.kinplatform.platform.enterprise.integration.ResolvedValue;
import com.kinplatform.platform.enterprise.integration.SupplementalData;
import com.kinplatform.platform.enterprise.web.dto.EnterpriseInformationResponse;
import com.kinplatform.platform.enterprise.web.dto.EnterpriseInformationResponse.DimensionInfo;
import com.kinplatform.platform.enterprise.web.dto.EnterpriseInformationResponse.DocumentInfo;
import com.kinplatform.platform.enterprise.web.dto.EnterpriseInformationResponse.SupplementalInfo;
import com.kinplatform.platform.enterprise.web.dto.EnterpriseInformationResponse.ValueInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint {@code GET /enterprise/{projectId}/information}: expone la vista de
 * integración (dimensiones resueltas + datos suplementarios + documentos) para
 * la sección "Información y fuentes" del dashboard. El ownership del proyecto
 * lo valida {@link EnterpriseOwnershipInterceptor} (misma garantía que el resto
 * de rutas {@code /enterprise/**}).
 */
@RestController
@RequestMapping({
    "/enterprise",
    "/empresas/enterprise"
})
@Tag(name = "Enterprise", description = "Información y fuentes del proyecto (integración)")
public class EnterpriseInformationController {

    private final EnterpriseDataProvider dataProvider;
    private final ContextRepository contextRepository;

    public EnterpriseInformationController(EnterpriseDataProvider dataProvider, ContextRepository contextRepository) {
        this.dataProvider = dataProvider;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/{projectId}/information")
    @Operation(
            summary = "Información y fuentes del proyecto",
            description = "Dimensiones resueltas, datos suplementarios y documentos del "
                    + "proyecto con origen y estado explícito (nunca convierte un dato "
                    + "ausente en 0).")
    public ResponseEntity<EnterpriseInformationResponse> information(@PathVariable UUID projectId) {
        ProjectContext context = contextRepository.find(projectId).orElse(null);
        EnterpriseIntegrationData data = dataProvider.load(projectId, context);
        return ResponseEntity.ok(toResponse(data));
    }

    private EnterpriseInformationResponse toResponse(EnterpriseIntegrationData data) {
        List<DimensionInfo> dimensions = new ArrayList<>();
        for (AnalyzedDimension dimension : AnalyzedDimension.values()) {
            ResolvedValue resolved = data.resolvedContext().value(dimension);
            dimensions.add(new DimensionInfo(
                    dimension.name(),
                    dimension.displayName(),
                    resolved.value(),
                    resolved.sourceType() != null ? resolved.sourceType().name() : null,
                    resolved.state().name(),
                    resolved.origin()));
        }
        SupplementalData supplemental = data.supplementalData();
        SupplementalInfo supplementalInfo = new SupplementalInfo(
                toValueMap(supplemental.financial()),
                toValueMap(supplemental.market()),
                toValueMap(supplemental.impact()),
                toValueMap(supplemental.risk()),
                toValueInfo(supplemental.breakeven().value()),
                supplemental.breakeven().missingFields(),
                supplemental.breakeven().calculable());
        List<DocumentInfo> documents =
                data.documents().stream().map(this::toDocumentInfo).toList();
        return new EnterpriseInformationResponse(
                List.copyOf(dimensions),
                supplementalInfo,
                documents,
                data.resolvedContext().conflicts());
    }

    private Map<String, ValueInfo> toValueMap(Map<String, ResolvedValue> values) {
        Map<String, ValueInfo> map = new LinkedHashMap<>();
        for (Map.Entry<String, ResolvedValue> entry : values.entrySet()) {
            map.put(entry.getKey(), toValueInfo(entry.getValue()));
        }
        return Map.copyOf(map);
    }

    private ValueInfo toValueInfo(ResolvedValue value) {
        return new ValueInfo(
                value.value(),
                value.sourceType() != null ? value.sourceType().name() : null,
                value.state().name(),
                value.origin());
    }

    private DocumentInfo toDocumentInfo(DocumentDescriptor document) {
        return new DocumentInfo(
                document.id(),
                document.filename(),
                document.mimeType(),
                document.size(),
                document.status(),
                document.createdAt(),
                document.summary(),
                document.relevantFacts());
    }
}



