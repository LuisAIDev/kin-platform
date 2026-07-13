package com.kinplatform.project;

import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.project.dto.ProjectResponse;
import com.kinplatform.project.dto.UpdateProjectRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProjectService {

    ProjectResponse create(UUID userId, CreateProjectRequest request);

    ProjectResponse getById(UUID userId, UUID projectId);

    PageResponse<ProjectResponse> getAllByUser(UUID userId, Pageable pageable);

    ProjectResponse update(UUID userId, UUID projectId, UpdateProjectRequest request);

    void delete(UUID userId, UUID projectId);
}
