package com.kinplatform.ai.usage;

import com.kinplatform.kin.usage.ProjectQuotaPort;
import com.kinplatform.kin.usage.UsagePeriod;
import com.kinplatform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de la cuota persistente de proyectos completados. Lee el contador
 * del usuario y aplica el rollover mensual de forma atómica. Eliminar un
 * proyecto no devuelve cupo: el contador solo se incrementa.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectQuotaAdapter implements ProjectQuotaPort {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void rolloverIfNeeded(UUID userId) {
        OffsetDateTime periodStart = UsagePeriod.current().start();
        int updated = userRepository.rolloverCompletedProjects(userId, periodStart);
        if (updated > 0) {
            log.info("Rollover de cuota de proyectos completados para usuario {} (periodo {})", userId, periodStart);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int completedProjects(UUID userId) {
        return userRepository
                .findById(userId)
                .map(u -> u.getCompletedProjects() != null ? u.getCompletedProjects() : 0)
                .orElse(0);
    }

    @Override
    @Transactional
    public boolean canComplete(UUID userId, Integer limit) {
        if (limit == null) {
            return true;
        }
        rolloverIfNeeded(userId);
        return completedProjects(userId) < limit;
    }

    @Override
    @Transactional
    public boolean tryIncrementCompleted(UUID userId, Integer limit) {
        return userRepository.tryIncrementCompletedProjects(userId, limit) == 1;
    }
}
