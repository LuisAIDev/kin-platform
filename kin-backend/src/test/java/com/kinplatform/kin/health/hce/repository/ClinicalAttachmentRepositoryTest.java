package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class ClinicalAttachmentRepositoryTest extends PostgresTestSupport {

    @Autowired
    private ClinicalAttachmentRepository repository;

    private UUID patientId;
    private UUID encounterId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        encounterId = UUID.randomUUID();
    }

    @Test
    void save_ShouldPersistAndRetrieve() {
        com.kinplatform.kin.health.hce.entity.ClinicalAttachment ca = com.kinplatform.kin.health.hce.entity.ClinicalAttachment.builder()
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                .loincCode("718-7")
                .resultValue(new java.math.BigDecimal("120"))
                .resultUnit("mg/dL")
                .referenceRangeLow(new java.math.BigDecimal("70"))
                .referenceRangeHigh(new java.math.BigDecimal("100"))
                .abnormalFlag(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag.HIGH)
                .performedAt(java.time.Instant.now())
                .build();

        com.kinplatform.kin.health.hce.entity.ClinicalAttachment saved = repository.save(ca);

        assertThat(saved.getId()).isNotNull();
        Optional<com.kinplatform.kin.health.hce.entity.ClinicalAttachment> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByLoincCode_ShouldFindLabResults() {
        UUID patientId = UUID.randomUUID();
        com.kinplatform.kin.health.hce.entity.ClinicalAttachment ca1 = com.kinplatform.kin.health.hce.entity.ClinicalAttachment.builder()
                .patientId(patientId).attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                .loincCode("718-7").resultValue(new java.math.BigDecimal("120"))
                .performedAt(java.time.Instant.now()).build();

        com.kinplatform.kin.health.hce.entity.ClinicalAttachment ca2 = com.kinplatform.kin.health.hce.entity.ClinicalAttachment.builder()
                .patientId(UUID.randomUUID()).attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING)
                .dicomStudyUid("1.2.3").performedAt(java.time.Instant.now()).build();

        repository.saveAll(java.util.List.of(ca1, ca2));

        List<com.kinplatform.kin.health.hce.entity.ClinicalAttachment> labs = repository.findByLoincCode("718-7");
        assertThat(labs).hasSize(1);
    }
}


