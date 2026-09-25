package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.Regimen;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PatientIdentificationRepositoryTest extends PostgresTestSupport {

    @Autowired
    private PatientIdentificationRepository repository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieveById() {
        PatientIdentification id = PatientIdentification.builder()
                .userId(userId)
                .documentType(com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.CC)
                .documentNumber("12345678")
                .rhFactor(com.kinplatform.kin.health.hce.entity.PatientIdentification.RhFactor.A_POS)
                .epsCode("EPS001")
                .regimen(Regimen.CONTRIBUTIVO)
                .emergencyContactName("Contacto")
                .emergencyContactPhone("3001234567")
                .build();

        PatientIdentification saved = repository.save(id);

        assertThat(saved.getId()).isNotNull();

        Optional<PatientIdentification> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDocumentNumber()).isEqualTo("12345678");
    }

    @Test
    void findByUserId_ShouldReturnOptional() {
        PatientIdentification id = PatientIdentification.builder()
                .userId(userId)
                .documentType(com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.CC)
                .documentNumber("12345678")
                .build();

        repository.save(id);

        Optional<PatientIdentification> found = repository.findByUserId(userId);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    void findByDocumentTypeAndDocumentNumber_ShouldFindExact() {
        PatientIdentification id = PatientIdentification.builder()
                .userId(UUID.randomUUID())
                .documentType(com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.CC)
                .documentNumber("87654321")
                .build();

        repository.save(id);

        Optional<PatientIdentification> found = repository.findByDocumentTypeAndDocumentNumber(
                com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.CC, "87654321");

        assertThat(found).isPresent();
        assertThat(found.get().getDocumentNumber()).isEqualTo("87654321");
    }

    @Test
    void documentNumber_ShouldBeUniquePerType() {
        PatientIdentification id1 = PatientIdentification.builder()
                .userId(UUID.randomUUID())
                .documentType(com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.CC)
                .documentNumber("11111111")
                .build();

        PatientIdentification id2 = PatientIdentification.builder()
                .userId(UUID.randomUUID())
                .documentType(com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.CC)
                .documentNumber("11111111")
                .build();

        repository.save(id1);

        assertThatThrownBy(() -> repository.saveAndFlush(id2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}


