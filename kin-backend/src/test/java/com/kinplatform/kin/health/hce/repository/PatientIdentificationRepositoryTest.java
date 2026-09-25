package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.Regimen;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientIdentificationRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private PatientIdentificationRepository repository;

    @Test
    void save_ShouldPersistAndRetrieveById() {
        PatientIdentification id = PatientIdentification.builder()
                .userId(patientId)
                .documentType(DocumentType.CC)
                .documentNumber("12345678")
                .rhFactor(PatientIdentification.RhFactor.A_POS)
                .epsCode("EPS001")
                .regimen(Regimen.CONTRIBUTIVO)
                .emergencyContactName("Contacto")
                .emergencyContactPhone("3001234567")
                .build();

        PatientIdentification saved = repository.saveAndFlush(id);

        assertThat(saved.getId()).isNotNull();
        Optional<PatientIdentification> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDocumentNumber()).isEqualTo("12345678");
    }

    @Test
    void findByUserId_ShouldReturnOptional() {
        repository.saveAndFlush(PatientIdentification.builder()
                .userId(patientId).documentType(DocumentType.CC).documentNumber("11112222").build());

        Optional<PatientIdentification> found = repository.findByUserId(patientId);
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(patientId);
    }

    @Test
    void findByDocumentTypeAndDocumentNumber_ShouldFindExact() {
        repository.saveAndFlush(PatientIdentification.builder()
                .userId(patientId).documentType(DocumentType.CC).documentNumber("87654321").build());

        Optional<PatientIdentification> found = repository.findByDocumentTypeAndDocumentNumber(
                DocumentType.CC, "87654321");

        assertThat(found).isPresent();
        assertThat(found.get().getDocumentNumber()).isEqualTo("87654321");
    }

    @Test
    void documentNumber_ShouldBeUniquePerUser() {
        repository.saveAndFlush(PatientIdentification.builder()
                .userId(patientId).documentType(DocumentType.CC).documentNumber("99999999").build());

        PatientIdentification duplicate = PatientIdentification.builder()
                .userId(patientId).documentType(DocumentType.CC).documentNumber("99999999").build();

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}