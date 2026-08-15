package com.kinplatform.projectdoc;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
