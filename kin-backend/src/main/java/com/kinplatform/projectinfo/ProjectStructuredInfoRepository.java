package com.kinplatform.projectinfo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectStructuredInfoRepository extends JpaRepository<ProjectStructuredInfo, ProjectStructuredInfoId> {

    List<ProjectStructuredInfo> findByProjectIdOrderBySectionAscKeyAsc(UUID projectId);
}
