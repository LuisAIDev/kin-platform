package com.kinplatform.project;

import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.project.dto.ProjectResponse;
import com.kinplatform.project.dto.UpdateProjectRequest;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse create(UUID userId, CreateProjectRequest request);

    ProjectResponse getById(UUID userId, UUID projectId);

    List<ProjectResponse> getAllByUser(UUID userId);

    ProjectResponse update(UUID userId, UUID projectId, UpdateProjectRequest request);

    void delete(UUID userId, UUID projectId);
}
