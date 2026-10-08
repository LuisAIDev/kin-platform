package com.kinplatform.common.legal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivacyPolicyVersionRepository extends JpaRepository<PrivacyPolicyVersion, UUID> {

    Optional<PrivacyPolicyVersion> findByActiveTrue();

    Optional<PrivacyPolicyVersion> findByVersion(String version);

    List<PrivacyPolicyVersion> findAllByOrderByEffectiveDateDesc();
}
