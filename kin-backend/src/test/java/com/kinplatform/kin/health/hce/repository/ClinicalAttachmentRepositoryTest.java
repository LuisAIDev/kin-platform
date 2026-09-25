package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalAttachmentRepositoryTest extends HceRepositoryTestSupport {

    @Autowired
    private ClinicalAttachmentRepository repository;

    @Test
    void save_ShouldPersistAndRetrieve() {
        ClinicalAttachment ca = ClinicalAttachment.builder()
                .patientId(patientId).encounterId(encounterId)
                .attachmentType(AttachmentType.LAB_RESULT).loincCode("718-7")
                .resultValue(BigDecimal.valueOf(120)).resultUnit("mg/dL")
                .referenceRangeLow(BigDecimal.valueOf(70)).referenceRangeHigh(BigDecimal.valueOf(100))
                .abnormalFlag(AbnormalFlag.HIGH).performedAt(Instant.now())
                .build();

        ClinicalAttachment saved = repository.saveAndFlush(ca);

        assertThat(saved.getId()).isNotNull();
        Optional<ClinicalAttachment> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
    }

    @Test
    void findByLoincCode_ShouldFindLabResults() {
        repository.saveAndFlush(ClinicalAttachment.builder()
                .patientId(patientId).attachmentType(AttachmentType.LAB_RESULT).loincCode("718-7")
                .resultValue(BigDecimal.valueOf(120)).performedAt(Instant.now()).build());
        repository.saveAndFlush(ClinicalAttachment.builder()
                .patientId(patientId).attachmentType(AttachmentType.IMAGING)
                .dicomStudyUid("1.2.3").performedAt(Instant.now()).build());

        List<ClinicalAttachment> labs = repository.findByLoincCode("718-7");
        assertThat(labs).hasSize(1);
    }
}