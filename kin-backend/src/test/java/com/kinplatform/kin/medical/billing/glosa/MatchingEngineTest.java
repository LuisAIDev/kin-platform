package com.kinplatform.kin.medical.billing.glosa;

import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingEngineTest {

    @Mock
    private RipsRecordRepository recordRepository;

    @InjectMocks
    private MatchingEngine matchingEngine;

    @Test
    void match_resolvesByRipsRecordId() {
        UUID recordId = UUID.randomUUID();
        RipsRecord record = RipsRecord.builder().id(recordId).build();
        when(recordRepository.findById(recordId)).thenReturn(Optional.of(record));

        Optional<RipsRecord> result = matchingEngine.match(
                Glosa.builder().ripsRecordId(recordId).build());

        assertTrue(result.isPresent());
        assertEquals(recordId, result.get().getId());
    }

    @Test
    void match_resolvesByBatchAndGlosaCode() {
        UUID batchId = UUID.randomUUID();
        RipsRecord other = RipsRecord.builder().id(UUID.randomUUID())
                .ripsLineData("{\"codigo_consulta\":\"111\"}").build();
        RipsRecord target = RipsRecord.builder().id(UUID.randomUUID())
                .ripsLineData("{\"codigo_consulta\":\"890201\"}").build();
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId)).thenReturn(List.of(other, target));

        Optional<RipsRecord> result = matchingEngine.match(
                Glosa.builder().ripsBatchId(batchId).glosaCode("890201").build());

        assertTrue(result.isPresent());
        assertEquals(target.getId(), result.get().getId());
    }

    @Test
    void match_returnsEmptyWhenNoData() {
        assertTrue(matchingEngine.match(Glosa.builder().build()).isEmpty());
    }
}


