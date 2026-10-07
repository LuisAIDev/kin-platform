package com.kinplatform.kin.medical.institutional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InstitutionalInquiryRepository extends JpaRepository<InstitutionalInquiry, UUID> {

    List<InstitutionalInquiry> findAllByOrderByCreatedAtDesc();

    List<InstitutionalInquiry> findByStatusOrderByCreatedAtDesc(InstitutionalInquiry.InquiryStatus status);
}

