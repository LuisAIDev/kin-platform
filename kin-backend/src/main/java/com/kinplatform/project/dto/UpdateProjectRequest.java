package com.kinplatform.project.dto;

import com.kinplatform.project.ProjectCategory;
import com.kinplatform.project.ProjectStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private ProjectCategory category;

    private ProjectStatus status;
}
