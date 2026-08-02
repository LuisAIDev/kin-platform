package com.kinplatform.kin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.context.adapter.JpaContextRepository;
import com.kinplatform.ai.context.adapter.ProjectContextEntity;
import com.kinplatform.ai.context.adapter.ProjectContextJpaRepository;
import com.kinplatform.ai.context.adapter.HeuristicContextAnalyzerAdapter;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.CompletenessEvaluator;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.EvaluationPolicies;
import com.kinplatform.kin.context.ExplorationPriority;
import com.kinplatform.kin.context.strategy.ConversationStrategist;
import com.kinplatform.kin.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.TurnResult;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.stage.AnalyzerStage;
import com.kinplatform.kin.pipeline.stage.EvaluatorStage;
import com.kinplatform.kin.pipeline.stage.EventStage;
import com.kinplatform.kin.pipeline.stage.StrategistStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Reproducción del "reinicio del contexto": guarda el contexto en un turno y
 * verifica si sobrevive a un "reopen" (nuevo orquestador, mismo repositorio).
 */
@ExtendWith(MockitoExtension.class)
class ProjectContextReopenReproTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProjectContextJpaRepository jpa;

    private final Map<UUID, ProjectContextEntity> store = new HashMap<>();
    private ContextRepository contextRepository;

    @BeforeEach
    void setUp() {
        when(jpa.findById(any())).thenAnswer(i ->
            Optional.ofNullable(store.get(i.getArgument(0))));
        when(jpa.save(any())).thenAnswer(i -> {
            ProjectContextEntity e = i.getArgument(0);
            store.put(e.getProjectId(), e);
            return e;
        });
        contextRepository = new JpaContextRepository(jpa, new ObjectMapper());
    }

    private Pipeline pipeline() {
        return new Pipeline(List.of(
            new AnalyzerStage(new HeuristicContextAnalyzerAdapter()),
            new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
            new StrategistStage(new ConversationStrategist(
                new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
            new EventStage()));
    }

    private ConversationOrchestrator orchestrator() {
        var kinMethod = new KinMethod(pipeline(), new InMemoryDomainEventBus(), contextRepository);
        return new ConversationOrchestrator(new HistoryWindow(), new DefaultTurnPolicy(),
            kinMethod, new ResponseGuard(), contextRepository);
    }

    private ConversationTurn turn(String message) {
        return new ConversationTurn(PROJECT_ID, USER_ID, message, List.of(),
            "Proyecto", "Descripción", "Software");
    }

    @Test
    void elContextoDebeSobrevivirALaReapertura() {
        var primerOrquestador = orchestrator();
        var primerTurno = primerOrquestador.orchestrate(turn(
            "el problema es que la gente pierde tiempo buscando estacionamiento."));

        assertEquals("que la gente pierde tiempo buscando estacionamiento",
            primerTurno.projectContext().value(AnalyzedDimension.PROBLEM));

        var orquestadorReabierto = orchestrator();
        TurnResult reabierto = orquestadorReabierto.orchestrate(turn("contame más"));

        assertNotNull(reabierto.projectContext());
        assertEquals("que la gente pierde tiempo buscando estacionamiento",
            reabierto.projectContext().value(AnalyzedDimension.PROBLEM));
        assertEquals("que la gente pierde tiempo buscando estacionamiento",
            contextRepository.findOrCreate(PROJECT_ID, "Proyecto", "Descripción", "Software")
                .value(AnalyzedDimension.PROBLEM));
    }

    @Test
    void elAnalizadorNoDebeSaltarDimensionesSembradas() {
        var primerOrquestador = orchestrator();
        var primerTurno = primerOrquestador.orchestrate(turn(
            "Mi proyecto se llama Cafetería El Café."));

        assertEquals("Cafetería El Café",
            primerTurno.projectContext().value(AnalyzedDimension.PROJECT_NAME));
    }
}
