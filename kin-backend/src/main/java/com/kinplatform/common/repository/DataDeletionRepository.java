package com.kinplatform.common.repository;

import com.kinplatform.common.entity.DataDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataDeletionRepository extends JpaRepository<DataDeletionRequest, UUID> {

    List<DataDeletionRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<DataDeletionRequest> findByStatus(String status);

    @Query("SELECT COUNT(r) FROM DataDeletionRequest r WHERE r.userId = :userId AND r.status IN :statuses")
    long countByUserIdAndStatusIn(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);
}