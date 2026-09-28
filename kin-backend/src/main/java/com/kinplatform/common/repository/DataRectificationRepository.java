package com.kinplatform.common.repository;

import com.kinplatform.common.entity.DataRectificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DataRectificationRepository extends JpaRepository<DataRectificationRequest, UUID> {

    List<DataRectificationRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<DataRectificationRequest> findByStatus(String status);

    @Query("SELECT COUNT(r) FROM DataRectificationRequest r WHERE r.userId = :userId AND r.status IN :statuses")
    long countByUserIdAndStatusIn(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);
}