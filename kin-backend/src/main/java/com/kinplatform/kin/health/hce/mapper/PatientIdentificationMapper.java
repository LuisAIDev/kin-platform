package com.kinplatform.kin.health.hce.mapper;

import com.kinplatform.kin.health.hce.dto.PatientIdentificationResponse;
import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import org.springframework.stereotype.Component;

@Component
public class PatientIdentificationMapper {

    public PatientIdentificationResponse toResponse(PatientIdentification entity) {
        return PatientIdentificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .documentType(entity.getDocumentType().name())
                .documentNumber(entity.getDocumentNumber())
                .documentExpeditionDate(entity.getDocumentExpeditionDate() != null ? entity.getDocumentExpeditionDate().toString() : null)
                .documentExpeditionPlace(entity.getDocumentExpeditionPlace())
                .rhFactor(entity.getRhFactor() != null ? entity.getRhFactor().name() : null)
                .epsCode(entity.getEpsCode())
                .epsName(entity.getEpsName())
                .regimen(entity.getRegimen() != null ? entity.getRegimen().name() : null)
                .guardianName(entity.getGuardianName())
                .guardianDocumentType(entity.getGuardianDocumentType() != null ? entity.getGuardianDocumentType().name() : null)
                .guardianDocumentNumber(entity.getGuardianDocumentNumber())
                .guardianPhone(entity.getGuardianPhone())
                .guardianRelationship(entity.getGuardianRelationship())
                .emergencyContactName(entity.getEmergencyContactName())
                .emergencyContactPhone(entity.getEmergencyContactPhone())
                .emergencyContactRelationship(entity.getEmergencyContactRelationship())
                .address(entity.getAddress())
                .cityCode(entity.getCityCode())
                .departmentCode(entity.getDepartmentCode())
                .zone(entity.getZone() != null ? entity.getZone().name() : null)
                .stratum(entity.getStratum())
                .emailInstitutional(entity.getEmailInstitutional())
                .phoneSecondary(entity.getPhoneSecondary())
                .ethnicity(entity.getEthnicity())
                .displacementVictim(entity.getDisplacementVictim())
                .disabilityCertificate(entity.getDisabilityCertificate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}