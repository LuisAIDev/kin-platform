package com.kinplatform.project.dto;

import com.kinplatform.project.ProjectStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 60, message = "Category code must not exceed 60 characters")
    private String category;

    private ProjectStatus status;
}
