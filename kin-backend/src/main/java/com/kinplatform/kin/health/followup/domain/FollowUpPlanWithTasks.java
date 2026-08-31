package com.kinplatform.kin.health.followup.domain;

import java.util.List;

/**
 * Plan de seguimiento con sus tareas (vista agregada para la API).
 */
public record FollowUpPlanWithTasks(FollowUpPlan plan, List<FollowUpTask> tasks) {

    public FollowUpPlanWithTasks {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
