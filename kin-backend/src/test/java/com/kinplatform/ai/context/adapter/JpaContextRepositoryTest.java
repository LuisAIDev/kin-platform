package com.kinplatform.ai.context.adapter;

import com.kinplatform.ai.context.adapter.ProjectContextEntity;
import com.kinplatform.ai.context.adapter.JpaContextRepository;
import com.kinplatform.ai.context.adapter.ProjectContextJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaContextRepositoryTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ProjectContextJpaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JpaContextRepository contextRepository;

    private JpaContextRepository repo() {
        if (contextRepository == null) {
            contextRepository = new JpaContextRepository(repository, objectMapper);
        }
        return contextRepository;
    }

    @Test
    void findOrCreate_deberiaSembrarDesdeElProyecto_cuandoNoExiste() {
        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var ctx = repo().findOrCreate(PROJECT_ID, "Mi App", "Descripción", "Software");

        assertNotNull(ctx);
        assertTrue(ctx.isDimensionCovered(AnalyzedDimension.PROJECT_NAME));
        assertTrue(ctx.isDimensionCovered(AnalyzedDimension.SECTOR));
        assertFalse(ctx.isDimensionCovered(AnalyzedDimension.SOLUTION));
        verify(repository).save(any(ProjectContextEntity.class));
    }

    @Test
    void findOrCreate_deberiaRestaurarElContextoExistente() {
        var original = ProjectContext.fromProject("Otro", "Otra desc", "Otro sector");
        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repo().save(PROJECT_ID, original);
        var captor = ArgumentCaptor.forClass(ProjectContextEntity.class);
        verify(repository).save(captor.capture());
        clearInvocations(repository);

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.of(captor.getValue()));
        var ctx = repo().findOrCreate(PROJECT_ID, "Otro", "Otra desc", "Otro sector");

        assertEquals("Otro", ctx.value(AnalyzedDimension.PROJECT_NAME));
        assertEquals("Otro sector", ctx.value(AnalyzedDimension.SECTOR));
        assertEquals(0, ctx.exchangeCount());
        verify(repository, never()).save(any());
    }

    @Test
    void saveYfind_deberianHacerRoundTripDelEstadoCompleto() {
        var ctx = ProjectContext.fromProject("Mi App", "Descripción", "Software");
        ctx.update(new AnalysisResult(Map.of(AnalyzedDimension.PROBLEM, "resolver el problema X")));
        ctx.attachDecision(ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "preguntar más"));
        ctx.markReportGenerated();

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repo().save(PROJECT_ID, ctx);

        var captor = ArgumentCaptor.forClass(ProjectContextEntity.class);
        verify(repository).save(captor.capture());
        var entity = captor.getValue();
        assertEquals(PROJECT_ID, entity.getProjectId());
        assertNotNull(entity.getContextData());

        when(repository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));
        var restored = repo().find(PROJECT_ID).orElseThrow();

        assertEquals(ctx.exchangeCount(), restored.exchangeCount());
        assertEquals(ctx.knownDimensionsCount(), restored.knownDimensionsCount());
        assertTrue(restored.isDimensionCovered(AnalyzedDimension.PROJECT_NAME));
        assertTrue(restored.isDimensionCovered(AnalyzedDimension.PROBLEM));
        assertFalse(restored.isDimensionCovered(AnalyzedDimension.SOLUTION));
        assertFalse(restored.isDimensionCovered(AnalyzedDimension.TARGET_CUSTOMER));
        assertEquals("Descripción", restored.value(AnalyzedDimension.SOLUTION));
        assertEquals("resolver el problema X", restored.value(AnalyzedDimension.PROBLEM));
        assertEquals(ConversationDecision.ask(AnalyzedDimension.PROBLEM, 9, "preguntar más"),
            restored.currentDecision());
        assertTrue(restored.reportGenerated());
    }

    @Test
    void delete_deberiaEliminarPorProjectId() {
        repo().delete(PROJECT_ID);
        verify(repository).deleteById(PROJECT_ID);
    }
}
