package com.kinplatform.billing.authorization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MipresTokenRepository extends JpaRepository<MipresToken, String> {

    Optional<MipresToken> findByNit(String nit);
}