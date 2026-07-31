package com.kinplatform.ai.context.adapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectContextJpaRepository extends JpaRepository<ProjectContextEntity, UUID> {
}
