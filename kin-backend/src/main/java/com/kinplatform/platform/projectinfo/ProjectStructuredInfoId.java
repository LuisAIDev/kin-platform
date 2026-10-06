package com.kinplatform.platform.projectinfo;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Clave compuesta de {@link ProjectStructuredInfo} (project_id, section, key). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStructuredInfoId implements Serializable {

    private UUID projectId;
    private String section;
    private String key;
}

