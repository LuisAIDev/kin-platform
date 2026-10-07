package com.kinplatform.ai.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.platform.usage.UsagePeriod;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifica la reserva ATÓMICA de presupuesto de IA en PostgreSQL real
 * (Testcontainers): una solicitud que supera el presupuesto restante se
 * rechaza y, con dos requests concurrentes, solo una puede consumir el
 * presupuesto restante (row lock).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class AiUsageJpaIntegrationTest extends PostgresTestSupport {

    @Autowired
    private AiUsageJpaRepository repo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private UUID user;
    private UsagePeriod period;

    @BeforeEach
    void setUp() {
        // Insertado en transacción REQUIRES_NEW: queda COMMITEADO para que los
        // hilos del test de concurrencia vean la fila (FK ai_usage.user_id).
        TransactionTemplate requiresNew = new TransactionTemplate(txManager);
        requiresNew.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        user = requiresNew.execute(status -> userRepository
                .saveAndFlush(User.builder()
                        .email("usage-" + UUID.randomUUID() + "@kin.test")
                        .passwordHash("x")
                        .fullName("Usage Test")
                        .role(UserRole.FREE)
                        .build())
                .getId());
        period = UsagePeriod.current();
    }

    private int reserve(BigDecimal budget, BigDecimal estimate) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        return tx.execute(status -> {
            repo.insertPeriodRowIfMissing(user, period.start(), period.end());
            return repo.tryReserve(user, period.start(), budget, estimate);
        });
    }

    @Test
    void reservaConsecutiva_noSuperaPresupuesto() {
        BigDecimal budget = new BigDecimal("0.25");

        assertEquals(1, reserve(budget, new BigDecimal("0.20")));
        assertEquals(0, reserve(budget, new BigDecimal("0.20")));
    }

    @Test
    void reservaYReconciliacion_acumulanYLiberan() {
        BigDecimal budget = new BigDecimal("6.25");
        assertEquals(1, reserve(budget, new BigDecimal("0.40")));

        new TransactionTemplate(txManager).execute(status -> {
            repo.recordActual(user, period.start(), new BigDecimal("0.40"), new BigDecimal("0.35"), 10_000L, 5_000L);
            return null;
        });

        var record = repo.findByUserIdAndPeriodStart(user, period.start()).orElseThrow();
        assertEquals(0, record.getEstimatedCostUsd().compareTo(new BigDecimal("0.35")));
        assertEquals(0, record.getReservedCostUsd().compareTo(BigDecimal.ZERO));
        assertEquals(15_000L, record.getTotalTokens());
        assertEquals(1, record.getRequestCount());
    }

    @Test
    void dosRequestsConcurrentes_soloUnoPasa() throws Exception {
        BigDecimal budget = new BigDecimal("0.25");
        BigDecimal estimate = new BigDecimal("0.20");
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Integer> task = () -> {
            barrier.await();
            return reserve(budget, estimate);
        };

        try {
            Future<Integer> a = pool.submit(task);
            Future<Integer> b = pool.submit(task);
            int[] results = {a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)};

            int success = 0;
            int rejected = 0;
            for (int r : results) {
                if (r == 1) {
                    success++;
                } else {
                    rejected++;
                }
            }
            assertEquals(1, success, "exactamente una reserva concurrente debe tener éxito");
            assertEquals(1, rejected, "la segunda reserva concurrente debe ser rechazada");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void filaDePeriodoEsUnicaPorUsuario() {
        new TransactionTemplate(txManager).execute(status -> {
            repo.insertPeriodRowIfMissing(user, period.start(), period.end());
            repo.insertPeriodRowIfMissing(user, period.start(), period.end());
            return null;
        });

        assertTrue(repo.findByUserIdAndPeriodStart(user, period.start()).isPresent());
    }
}

