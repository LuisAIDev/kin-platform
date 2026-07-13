package com.kinplatform.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Project> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Project> findByUserIdAndCategoryOrderByCreatedAtDesc(UUID userId, ProjectCategory category);

    long countByUserId(UUID userId);
}
