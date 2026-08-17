package com.kinplatform.ai.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.usage.UsagePeriod;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.project.ProjectStatus;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * REGRESIÓN — cuota FREE persistente por período.
 *
 * <p>Regla de negocio: FREE = 3 proyectos COMPLETED por período. Crear un
 * DRAFT no consume; completar consume 1 unidad; el contador queda persistido
 * en {@code users.completed_projects}; BORRAR un proyecto NO devuelve la cuota
 * (se mantiene 3/3); el rollover del siguiente período reinicia la cuota; la
 * operación es atómica ante concurrencia; STANDARD = 5; PREMIUM = ilimitado.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(ProjectQuotaAdapter.class)
@ActiveProfiles("test")
class ProjectQuotaFreeRegressionTest extends PostgresTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectQuotaAdapter quotaAdapter;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("quota-free-" + UUID.randomUUID() + "@kin.test")
                .passwordHash("x")
                .fullName("Quota Free")
                .role(UserRole.FREE)
                .build());
    }

    private Project project(ProjectStatus status) {
        return projectRepository.save(Project.builder()
                .user(user)
                .title("P")
                .status(status)
                .build());
    }

    private int counter() {
        return userRepository.findById(user.getId()).orElseThrow().getCompletedProjects();
    }

    @Test
    void free_tresTres_borrarProyectoNoDevuelveCupo() {
        // 3 unidades consumidas en el período (persistido).
        user.setCompletedProjects(3);
        user.setCompletedProjectsPeriodStart(UsagePeriod.current().start());
        userRepository.saveAndFlush(user);
        Project p1 = project(ProjectStatus.COMPLETED);
        project(ProjectStatus.COMPLETED);
        project(ProjectStatus.COMPLETED);

        // Borrar una fila COMPLETED (borrado físico).
        projectRepository.delete(p1);
        projectRepository.flush();

        // El contador NO decrementa: sigue 3/3 y el cupo sigue agotado.
        assertEquals(3, counter(), "borrar una fila COMPLETED no debe decrementar users.completed_projects");
        assertFalse(quotaAdapter.canComplete(user.getId(), 3));
        assertFalse(quotaAdapter.tryIncrementCompleted(user.getId(), 3));
    }

    @Test
    void free_crearDraftNoConsume_completarConsume() {
        assertEquals(0, counter());
        project(ProjectStatus.DRAFT);
        project(ProjectStatus.IN_PROGRESS);
        assertEquals(0, counter(), "DRAFT/IN_PROGRESS no consumen cuota");

        project(ProjectStatus.COMPLETED);
        assertTrue(quotaAdapter.tryIncrementCompleted(user.getId(), 3));
        assertEquals(1, counter());
    }

    @Test
    void free_rolloverMensual_reiniciaCuota() {
        user.setCompletedProjects(3);
        // Período anterior (rollover al consultar).
        user.setCompletedProjectsPeriodStart(UsagePeriod.current().start().minusMonths(1));
        userRepository.saveAndFlush(user);

        assertTrue(quotaAdapter.canComplete(user.getId(), 3), "rollover del nuevo período libera la cuota");
        assertEquals(0, counter(), "rollover reinicia el contador a 0");
    }

    @Test
    void standard_limite5_premium_ilimitado() {
        // STANDARD (limit 5): consume 5 -> el 6.º se rechaza.
        for (int i = 0; i < 5; i++) {
            assertTrue(quotaAdapter.tryIncrementCompleted(user.getId(), 5), "cupo STANDARD disponible");
        }
        assertEquals(5, counter());
        assertFalse(quotaAdapter.tryIncrementCompleted(user.getId(), 5), "6.º completado rechazado en STANDARD");

        // PREMIUM (limit null): ilimitado.
        assertTrue(quotaAdapter.canComplete(user.getId(), null));
        assertTrue(quotaAdapter.tryIncrementCompleted(user.getId(), null));
    }
}
