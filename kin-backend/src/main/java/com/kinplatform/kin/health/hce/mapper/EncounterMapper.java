package com.kinplatform.kin.health.hce.mapper;

import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import org.springframework.stereotype.Component;

@Component
public class EncounterMapper {

    public EncounterResponse toResponse(Encounter e) {
        return EncounterResponse.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .physicianId(e.getPhysicianId())
                .organizationId(e.getOrganizationId())
                .appointmentId(e.getAppointmentId())
                .encounterType(e.getEncounterType().name())
                .status(e.getStatus().name())
                .chiefComplaint(e.getChiefComplaint())
                .startedAt(e.getStartedAt())
                .closedAt(e.getClosedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}