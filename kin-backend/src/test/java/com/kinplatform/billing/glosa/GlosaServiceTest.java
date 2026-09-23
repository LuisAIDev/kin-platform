package com.kinplatform.billing.glosa;

import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GlosaServiceTest {

    private GlosaRepository glosaRepository;
    private EpsContractRepository contractRepository;
    private GlosaParser glosaParser;
    private MatchingEngine matchingEngine;
    private GlosaService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID contractId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        glosaRepository = mock(GlosaRepository.class);
        contractRepository = mock(EpsContractRepository.class);
        glosaParser = mock(GlosaParser.class);
        matchingEngine = mock(MatchingEngine.class);
        service = new GlosaService(glosaRepository, contractRepository, glosaParser, matchingEngine,
                new AppealWorkflow());
        TenantContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void importFromFile_setsTenantContractAndDeadline() {
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId))
                .thenReturn(Optional.of(EpsContract.builder().id(contractId).build()));
        when(glosaParser.parse(anyString()))
                .thenReturn(List.of(Glosa.builder().epsGlosaNumber("G-1").build()));
        when(glosaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Glosa> glosas = service.importFromFile(contractId, "G-1||||VALOR|X|d|1|1");

        assertEquals(1, glosas.size());
        assertEquals(orgId, glosas.get(0).getOrganizationId());
        assertEquals(contractId, glosas.get(0).getContractId());
        assertNotNull(glosas.get(0).getAppealDeadline());
    }

    @Test
    void importFromFile_throwsWhenContractNotInTenant() {
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.importFromFile(contractId, "G-1||||VALOR|X|d|1|1"));
    }

    @Test
    void analyze_transitionsReceivedToAnalyzing() {
        Glosa glosa = Glosa.builder().id(UUID.randomUUID()).status(Glosa.GlosaStatus.RECEIVED).build();
        when(glosaRepository.findByIdAndOrganizationId(glosa.getId(), orgId)).thenReturn(Optional.of(glosa));
        when(glosaRepository.save(any(Glosa.class))).thenAnswer(inv -> inv.getArgument(0));

        Glosa result = service.analyze(glosa.getId());

        assertEquals(Glosa.GlosaStatus.ANALYZING, result.getStatus());
    }

    @Test
    void submitAppeal_setsAppealedAndArguments() {
        Glosa glosa = Glosa.builder().id(UUID.randomUUID()).status(Glosa.GlosaStatus.APPEALING).build();
        when(glosaRepository.findByIdAndOrganizationId(glosa.getId(), orgId)).thenReturn(Optional.of(glosa));
        when(glosaRepository.save(any(Glosa.class))).thenAnswer(inv -> inv.getArgument(0));

        Glosa result = service.submitAppeal(glosa.getId(), "Argumentos de apelacion");

        assertEquals(Glosa.GlosaStatus.APPEALED, result.getStatus());
        assertEquals("Argumentos de apelacion", result.getAppealArguments());
        assertNotNull(result.getAppealSubmittedAt());
    }

    @Test
    void resolveAccepted_setsResolvedValue() {
        Glosa glosa = Glosa.builder().id(UUID.randomUUID()).status(Glosa.GlosaStatus.ANALYZING).build();
        when(glosaRepository.findByIdAndOrganizationId(glosa.getId(), orgId)).thenReturn(Optional.of(glosa));
        when(glosaRepository.save(any(Glosa.class))).thenAnswer(inv -> inv.getArgument(0));

        Glosa result = service.resolve(glosa.getId(), Glosa.GlosaStatus.ACCEPTED, new BigDecimal("500"));

        assertEquals(Glosa.GlosaStatus.ACCEPTED, result.getStatus());
        assertEquals(new BigDecimal("500"), result.getResolvedValueCop());
        assertNotNull(result.getResolutionDate());
    }

    @Test
    void get_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(glosaRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.get(id));
    }
}
