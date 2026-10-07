package com.kinplatform.kin.medical.billing.rips.generator;

import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import com.kinplatform.common.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class UsGenerator implements RipsGenerator {

    @Override
    public RipsBatch.RipsType getType() {
        return RipsBatch.RipsType.US;
    }

    @Override
    public List<RipsRecord> generate(RipsGenerationContext context) {
        return context.patients().stream()
                .map(patient -> {
                    RipsRecord r = new RipsRecord();
                    r.setSourceEntityType("PATIENT");
                    r.setSourceEntityId(patient.getId());
                    // Use available User fields
                    String docType = "CC"; // default
                    String docNumber = "0000000000"; // placeholder
                    String fullName = patient.getFullName() != null ? patient.getFullName() : "Unknown";
                    String[] nameParts = fullName.split(" ");
                    String firstName = nameParts.length > 0 ? nameParts[0] : "";
                    String lastName = nameParts.length > 1 ? nameParts[1] : "";
                    
                    r.setRipsLineData(String.format(
                        "{\"tipo_documento\":\"CC\",\"numero_documento\":\"%s\",\"primer_nombre\":\"%s\",\"primer_apellido\":\"%s\"}",
                        patient.getId().toString().substring(0, 10),
                        firstName,
                        lastName
                    ));
                    return r;
                })
                .toList();
    }
}

