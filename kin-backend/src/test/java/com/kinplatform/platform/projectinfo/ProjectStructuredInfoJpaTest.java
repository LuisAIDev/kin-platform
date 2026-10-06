package com.kinplatform.platform.projectinfo;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.platform.project.Project;
import com.kinplatform.platform.project.ProjectRepository;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Round-trip real de {@code project_structured_info} contra PostgreSQL 18
 * (Testcontainers, Flyway V1..V14): verifica clave compuesta, FK al proyecto y
 * preservación del {@link StructuredInfoSourceType}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class ProjectStructuredInfoJpaTest extends PostgresTestSupport {

    @Autowired
    private ProjectStructuredInfoRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void roundTrip_persisteYRecuperaInformacionConOrigen() {
        User user = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("structured@kin.com")
                .passwordHash("hash")
                .fullName("Test")
                .role(UserRole.FREE)
                .build());
        Project project = projectRepository.save(Project.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Proyecto con info")
                .build());

        repository.save(ProjectStructuredInfo.builder()
                .projectId(project.getId())
                .section("FINANZAS")
                .key("inversion_inicial")
                .value("80000000")
                .sourceType(StructuredInfoSourceType.USER_INPUT)
                .build());

        var found = repository.findByProjectIdOrderBySectionAscKeyAsc(project.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSection()).isEqualTo("FINANZAS");
        assertThat(found.get(0).getKey()).isEqualTo("inversion_inicial");
        assertThat(found.get(0).getValue()).isEqualTo("80000000");
        assertThat(found.get(0).getSourceType()).isEqualTo(StructuredInfoSourceType.USER_INPUT);
    }

    @Test
    void roundTripConfirmacionConservaTrazabilidad() {
        User user = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email("confirm@kin.com")
                .passwordHash("hash")
                .fullName("Test")
                .role(UserRole.FREE)
                .build());
        Project project = projectRepository.save(Project.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Proyecto con confirmación")
                .build());

        repository.save(ProjectStructuredInfo.builder()
                .projectId(project.getId())
                .section("FINANZAS")
                .key("precio")
                .value("45000")
                .sourceType(StructuredInfoSourceType.USER_INPUT)
                .originalSourceType(StructuredInfoSourceType.IMPORTED_DOCUMENT)
                .sourceDocument("documento.pdf")
                .confirmedAt(java.time.OffsetDateTime.now())
                .build());

        var found = repository.findByProjectIdOrderBySectionAscKeyAsc(project.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOriginalSourceType()).isEqualTo(StructuredInfoSourceType.IMPORTED_DOCUMENT);
        assertThat(found.get(0).getSourceDocument()).isEqualTo("documento.pdf");
        assertThat(found.get(0).getConfirmedAt()).isNotNull();
    }
}


