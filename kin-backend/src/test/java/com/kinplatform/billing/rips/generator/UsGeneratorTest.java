package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.rips.generator.RipsGenerationContext;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsGeneratorTest {

    @InjectMocks
    private UsGenerator generator;

    @Test
    void generate_returnsRecordsForPatients() {
        RipsGenerationContext context = new RipsGenerationContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().minusMonths(1),
                LocalDate.now(),
                List.of(),
                List.of(),
                List.of(
                        User.builder().id(UUID.randomUUID()).fullName("Juan Perez").build(),
                        User.builder().id(UUID.randomUUID()).fullName("Maria Gomez").build()
                ),
                List.of(),
                Map.of(),
                Map.of()
        );

        List<RipsRecord> records = generator.generate(context);

        assertEquals(2, records.size());
        assertEquals("PATIENT", records.get(0).getSourceEntityType());
        assertTrue(records.get(0).getRipsLineData().contains("primer_nombre"));
    }

    @Test
    void supports_onlyUsType() {
        assertTrue(generator.supports(com.kinplatform.billing.rips.model.RipsBatch.RipsType.US));
        assertFalse(generator.supports(com.kinplatform.billing.rips.model.RipsBatch.RipsType.AF));
    }
}