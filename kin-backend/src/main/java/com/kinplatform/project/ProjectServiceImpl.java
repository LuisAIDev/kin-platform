package com.kinplatform.project;

import com.kinplatform.chat.ChatMessageRepository;
import com.kinplatform.chat.MessageRole;
import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.project.dto.ProjectResponse;
import com.kinplatform.project.dto.UpdateProjectRequest;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional
    public ProjectResponse create(UUID userId, CreateProjectRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var project = Project.builder()
                .user(user)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(request.getCategory())
                .status(ProjectStatus.DRAFT)
                .build();

        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID userId, UUID projectId) {
        var project = findOwnedProject(userId, projectId);
        return toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAllByUser(UUID userId, Pageable pageable) {
        Page<Project> page = projectRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public ProjectResponse update(UUID userId, UUID projectId, UpdateProjectRequest request) {
        var project = findOwnedProject(userId, projectId);

        if (request.getTitle() != null) {
            project.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            project.setCategory(request.getCategory());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID projectId) {
        var project = findOwnedProject(userId, projectId);
        projectRepository.delete(project);
    }

    private Project findOwnedProject(UUID userId, UUID projectId) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project does not belong to this user");
        }

        return project;
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .userId(project.getUser().getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .category(project.getCategory())
                .status(project.getStatus())
                .viabilityScore(project.getViabilityScore())
                .aiSummary(project.getAiSummary())
                .startedAt(project.getStartedAt())
                .completedAt(project.getCompletedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .progressPercentage(calculateProgress(project))
                .build();
    }

    private int calculateProgress(Project project) {
        int progress = 0;

        // 1. Basic info complete (20%)
        if (project.getTitle() != null && !project.getTitle().isBlank()
                && project.getDescription() != null && !project.getDescription().isBlank()
                && project.getCategory() != null) {
            progress += 20;
        }

        // 2. Viability score exists (20%)
        if (project.getViabilityScore() != null) {
            progress += 20;
        }

        long userMsgCount = chatMessageRepository.countByProjectIdAndRole(project.getId(), MessageRole.USER);

        // 3. At least 3 user messages (30%)
        if (userMsgCount >= 3) {
            progress += 30;
        }

        // 4. At least 6 user messages as proxy for value proposition (20%)
        if (userMsgCount >= 6) {
            progress += 20;
        }

        // 5. Status changed from DRAFT (10%)
        if (project.getStatus() != ProjectStatus.DRAFT) {
            progress += 10;
        }

        return progress;
    }
}
