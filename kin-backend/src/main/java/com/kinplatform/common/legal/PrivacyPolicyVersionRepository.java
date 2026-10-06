package com.kinplatform.common.legal;

import com.kinplatform.common.legal.PrivacyPolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrivacyPolicyVersionRepository extends JpaRepository<PrivacyPolicyVersion, UUID> {

    Optional<PrivacyPolicyVersion> findByActiveTrue();

    Optional<PrivacyPolicyVersion> findByVersion(String version);

    List<PrivacyPolicyVersion> findAllByOrderByEffectiveDateDesc();
}

