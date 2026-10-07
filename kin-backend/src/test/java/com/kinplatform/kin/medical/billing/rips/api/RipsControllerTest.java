package com.kinplatform.kin.medical.billing.rips.api;

import com.kinplatform.kin.medical.billing.rips.GenerateRipsRequest;
import com.kinplatform.kin.medical.billing.rips.RipsGenerationService;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatchRepository;
import com.kinplatform.common.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RipsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RipsGenerationService ripsService;

    @Mock
    private RipsBatchRepository batchRepository;

    private UUID orgId;
    private UUID contractId;
    private UUID batchId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        contractId = UUID.randomUUID();
        batchId = UUID.randomUUID();
        TenantContext.set(orgId);

        var controller = new com.kinplatform.kin.medical.billing.rips.RipsController(ripsService, batchRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void generate_returns201() throws Exception {
        RipsBatch batch = createBatch(batchId, orgId, contractId, RipsBatch.BatchStatus.VALID);

        when(ripsService.generateAllTypes(eq(orgId), eq(contractId), any(), any()))
            .thenReturn(batch);

        mockMvc.perform(post("/billing/rips/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"contractId":"%s","periodStart":"2026-09-01","periodEnd":"2026-09-30"}
                    """.formatted(contractId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(batchId.toString()))
            .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test
    void getBatch_returns200() throws Exception {
        RipsBatch batch = createBatch(batchId, orgId, contractId, RipsBatch.BatchStatus.VALID);

        when(batchRepository.findByIdAndOrganizationId(batchId, orgId))
            .thenReturn(Optional.of(batch));

        mockMvc.perform(get("/billing/rips/{batchId}", batchId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(batchId.toString()))
            .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test
    void getBatch_fromOtherOrg_returns404() throws Exception {
        when(batchRepository.findByIdAndOrganizationId(batchId, orgId))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/billing/rips/{batchId}", batchId))
            .andExpect(status().isNotFound());
    }

    @Test
    void download_returns200WithZip() throws Exception {
        RipsBatch batch = createBatch(batchId, orgId, contractId, RipsBatch.BatchStatus.VALID);
        byte[] zipContent = "test zip content".getBytes();

        when(batchRepository.findByIdAndOrganizationId(batchId, orgId))
            .thenReturn(Optional.of(batch));
        when(ripsService.downloadAsZip(orgId, batchId))
            .thenReturn(zipContent);

        mockMvc.perform(get("/billing/rips/{batchId}/download", batchId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"rips_" + batchId + ".zip\""))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().bytes(zipContent));
    }

    @Test
    void retry_invalidBatch_returns200() throws Exception {
        RipsBatch batch = createBatch(batchId, orgId, contractId, RipsBatch.BatchStatus.INVALID);
        RipsBatch retriedBatch = createBatch(batchId, orgId, contractId, RipsBatch.BatchStatus.VALID);

        when(batchRepository.findByIdAndOrganizationId(batchId, orgId))
            .thenReturn(Optional.of(batch));
        when(ripsService.retry(orgId, batchId))
            .thenReturn(retriedBatch);

        mockMvc.perform(post("/billing/rips/{batchId}/retry", batchId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test
    void retry_validBatch_returns400() throws Exception {
        when(ripsService.retry(orgId, batchId))
            .thenThrow(new IllegalStateException("Solo se pueden reintentar batches INVALID"));

        mockMvc.perform(post("/billing/rips/{batchId}/retry", batchId))
            .andExpect(status().isBadRequest());
    }

    private RipsBatch createBatch(UUID id, UUID organizationId, UUID contractId, RipsBatch.BatchStatus status) {
        return RipsBatch.builder()
            .id(id)
            .organizationId(organizationId)
            .contractId(contractId)
            .periodStart(LocalDate.of(2026, 9, 1))
            .periodEnd(LocalDate.of(2026, 9, 30))
            .ripsType(RipsBatch.RipsType.US)
            .status(status)
            .recordCount(10)
            .errorCount(0)
            .generatedAt(OffsetDateTime.now())
            .validatedAt(OffsetDateTime.now())
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }
}


