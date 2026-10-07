package com.kinplatform.common.ai.interview.adapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InterviewStateJpaRepository extends JpaRepository<InterviewStateEntity, UUID> {
}

