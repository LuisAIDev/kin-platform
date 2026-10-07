package com.kinplatform.common.legal;

import com.kinplatform.common.legal.PrivacyPolicyVersion;
import com.kinplatform.common.legal.PrivacyPolicyVersionRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrivacyPolicyServiceTest {

    @Mock
    private PrivacyPolicyVersionRepository policyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.kinplatform.common.audit.adapter.AuditLogJpaRepository auditRepository;

    private PrivacyPolicyService service;

    private User adminUser;
    private UUID adminId;
    private UUID versionId;
    private PrivacyPolicyVersion activePolicy;
    private PrivacyPolicyVersion oldPolicy;

    @BeforeEach
    void setUp() {
        service = new PrivacyPolicyService(policyRepository, userRepository, auditRepository);

        adminId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        adminUser = User.builder()
            .id(adminId)
            .email("admin@example.com")
            .fullName("Admin User")
            .passwordHash("hash")
            .role(com.kinplatform.common.user.UserRole.ADMIN)
            .build();

        activePolicy = PrivacyPolicyVersion.builder()
            .id(versionId)
            .version("1.0")
            .title("Política de Privacidad v1.0")
            .contentMd("# Política v1.0\nContenido...")
            .effectiveDate(java.time.LocalDate.of(2024, 1, 1))
            .publishedAt(Instant.now().minusSeconds(86400))
            .publishedBy(adminId)
            .active(true)
            .build();

        oldPolicy = PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID())
            .version("0.9")
            .title("Política de Privacidad v0.9")
            .contentMd("# Política v0.9\nContenido anterior...")
            .effectiveDate(java.time.LocalDate.of(2023, 1, 1))
            .publishedAt(Instant.now().minusSeconds(172800))
            .publishedBy(adminId)
            .active(false)
            .build();
    }

    @Test
    void getActivePolicy_returnsActive() {
        when(policyRepository.findByActiveTrue()).thenReturn(Optional.of(activePolicy));

        Optional<PrivacyPolicyVersion> result = service.getActivePolicy();

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo("1.0");
        assertThat(result.get().getActive()).isTrue();
    }

    @Test
    void getActivePolicy_noActive_returnsEmpty() {
        when(policyRepository.findByActiveTrue()).thenReturn(Optional.empty());

        Optional<PrivacyPolicyVersion> result = service.getActivePolicy();

        assertThat(result).isEmpty();
    }

    @Test
    void getPolicyByVersion_returnsCorrectVersion() {
        when(policyRepository.findByVersion("1.0")).thenReturn(Optional.of(activePolicy));

        Optional<PrivacyPolicyVersion> result = service.getPolicyByVersion("1.0");

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo("1.0");
    }

    @Test
    void getPolicyByVersion_notFound_returnsEmpty() {
        when(policyRepository.findByVersion("2.0")).thenReturn(Optional.empty());

        Optional<PrivacyPolicyVersion> result = service.getPolicyByVersion("2.0");

        assertThat(result).isEmpty();
    }

    @Test
    void listAllVersions_returnsOrdered() {
        PrivacyPolicyVersion v1 = PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID()).version("1.0").title("v1.0")
            .effectiveDate(java.time.LocalDate.of(2024, 1, 1)).active(true).build();
        PrivacyPolicyVersion v2 = PrivacyPolicyVersion.builder()
            .id(UUID.randomUUID()).version("2.0").title("v2.0")
            .effectiveDate(java.time.LocalDate.of(2025, 1, 1)).active(true).build();

        when(policyRepository.findAllByOrderByEffectiveDateDesc()).thenReturn(List.of(v2, v1));

        List<PrivacyPolicyVersion> versions = service.listAllVersions();

        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).getEffectiveDate()).isEqualTo(java.time.LocalDate.of(2025, 1, 1));
        assertThat(versions.get(1).getEffectiveDate()).isEqualTo(java.time.LocalDate.of(2024, 1, 1));
    }

    @Test
    void publishNewVersion_deactivatesOld() {
        when(policyRepository.findByActiveTrue()).thenReturn(Optional.of(activePolicy));
        when(policyRepository.findByVersion("2.0")).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.of(adminUser));
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrivacyPolicyVersion result = service.publishNewVersion("2.0", "Política v2.0", "# Nueva política", java.time.LocalDate.now(), UUID.randomUUID());

        assertThat(result.getVersion()).isEqualTo("2.0");
        assertThat(result.getActive()).isTrue();
        assertThat(activePolicy.getActive()).isFalse();
    }

    @Test
    void publishNewVersion_createsNewActive() {
        when(policyRepository.findByActiveTrue()).thenReturn(Optional.of(activePolicy));
        when(policyRepository.findByVersion("2.0")).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.of(adminUser));
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrivacyPolicyVersion result = service.publishNewVersion("2.0", "Política v2.0", "# Nueva política", java.time.LocalDate.now(), UUID.randomUUID());

        assertThat(result.getVersion()).isEqualTo("2.0");
        assertThat(result.getActive()).isTrue();
        assertThat(result.getTitle()).isEqualTo("Política v2.0");
        assertThat(result.getContentMd()).isEqualTo("# Nueva política");
    }

    @Test
    void publishNewVersion_duplicateVersion_throwsException() {
        when(policyRepository.findByVersion("1.0")).thenReturn(Optional.of(activePolicy));

        assertThatThrownBy(() -> service.publishNewVersion("1.0", "Duplicada", "# Contenido", java.time.LocalDate.now(), UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya existe");
    }

    @Test
    void getPolicyHistory_returnsAll() {
        when(policyRepository.findAllByOrderByEffectiveDateDesc()).thenReturn(List.of(activePolicy, oldPolicy));

        List<PrivacyPolicyVersion> history = service.getPolicyHistory();

        assertThat(history).hasSize(2);
    }
}




