package com.kinplatform.platform.ai_enterprise.integration;

import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.platform.enterprise.integration.DocumentDescriptor;
import com.kinplatform.platform.enterprise.integration.DocumentRelevance;
import com.kinplatform.platform.enterprise.integration.EnterpriseDataProvider;
import com.kinplatform.platform.enterprise.integration.EnterpriseIntegrationData;
import com.kinplatform.platform.enterprise.integration.ResolvedContext;
import com.kinplatform.platform.enterprise.integration.StructuredDatum;
import com.kinplatform.platform.enterprise.integration.SupplementalData;
import com.kinplatform.platform.projectdoc.ProjectDocument;
import com.kinplatform.platform.projectdoc.ProjectDocumentRepository;
import com.kinplatform.platform.projectdoc.ProjectDocumentStatus;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Adaptador de infraestructura de {@link EnterpriseDataProvider}: lee
 * {@code project_structured_info} y {@code project_documents} y construye la
 * vista de integración. El texto extraído se usa únicamente para resúmenes y
 * hechos relevantes; nunca se expone completo.
 */
@Service
public class JpaEnterpriseDataProvider implements EnterpriseDataProvider {

    private static final int SUMMARY_MAX_CHARS = 300;

    private final ProjectStructuredInfoRepository structuredInfoRepository;
    private final ProjectDocumentRepository documentRepository;

    public JpaEnterpriseDataProvider(
            ProjectStructuredInfoRepository structuredInfoRepository, ProjectDocumentRepository documentRepository) {
        this.structuredInfoRepository = structuredInfoRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    public EnterpriseIntegrationData load(UUID projectId, ProjectContext context) {
        List<StructuredDatum> data = structuredInfoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId).stream()
                .map(this::toDatum)
                .toList();

        List<DocumentDescriptor> documents = documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toDescriptor)
                .toList();

        return new EnterpriseIntegrationData(
                ResolvedContext.resolve(context, data), SupplementalData.from(data), documents);
    }

    private StructuredDatum toDatum(ProjectStructuredInfo entity) {
        return new StructuredDatum(entity.getSection(), entity.getKey(), entity.getValue(), entity.getSourceType());
    }

    private DocumentDescriptor toDescriptor(ProjectDocument document) {
        boolean processed = document.getStatus() == ProjectDocumentStatus.PROCESADO;
        String summary = processed ? DocumentRelevance.summarize(document.getExtractedText(), SUMMARY_MAX_CHARS) : null;
        List<String> relevant =
                processed ? DocumentRelevance.findRelevant(document.getExtractedText()) : new ArrayList<>();
        return new DocumentDescriptor(
                document.getId().toString(),
                document.getFilename(),
                document.getMimeType(),
                document.getSize(),
                document.getStatus().name(),
                document.getCreatedAt() != null ? document.getCreatedAt().toString() : null,
                summary,
                relevant);
    }
}
