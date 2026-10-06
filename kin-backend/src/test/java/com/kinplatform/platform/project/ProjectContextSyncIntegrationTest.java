package com.kinplatform.platform.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.ai.context.adapter.HeuristicContextAnalyzerAdapter;
import com.kinplatform.ai.context.adapter.JpaContextRepository;
import com.kinplatform.ai.context.adapter.ProjectContextEntity;
import com.kinplatform.ai.context.adapter.ProjectContextJpaRepository;
import com.kinplatform.platform.chat.ChatMessageRepository;
import com.kinplatform.kin.KinMethod;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.CompletenessEvaluator;
import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.EvaluationPolicies;
import com.kinplatform.common.context.ExplorationPriority;
import com.kinplatform.common.context.ProjectContextSyncPort;
import com.kinplatform.common.context.strategy.ConversationStrategist;
import com.kinplatform.common.context.strategy.DefaultExplorationStrategy;
import com.kinplatform.common.conversation.ConversationOrchestrator;
import com.kinplatform.common.conversation.ConversationTurn;
import com.kinplatform.common.conversation.TurnResult;
import com.kinplatform.common.conversation.history.HistoryWindow;
import com.kinplatform.common.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.common.conversation.validation.ResponseGuard;
import com.kinplatform.common.pipeline.Pipeline;
import com.kinplatform.common.pipeline.stage.AnalyzerStage;
import com.kinplatform.common.pipeline.stage.EvaluatorStage;
import com.kinplatform.common.pipeline.stage.EventStage;
import com.kinplatform.common.pipeline.stage.StrategistStage;
import com.kinplatform.pricing.service.SubscriptionValidatorService;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integración de la sincronización ProjectContext → Project (FASE 9 · cierre de
 * la desincronización): el Analizador captura el nombre/problema, la entidad
 * Project se sincroniza y el Dashboard deja de mostrar 0 %.
 */
@ExtendWith(MockitoExtension.class)
class ProjectContextSyncIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SubscriptionValidatorService subscriptionValidatorService;

    @Mock
    private ProjectContextJpaRepository contextJpa;

    @Mock
    private CategoryRepository categoryRepository;

    private final Map<UUID, ProjectContextEntity> contextStore = new HashMap<>();
    private Project project;
    private ContextRepository contextRepository;
    private ProjectContextSyncPort sync;

    @BeforeEach
    void setUp() {
        when(contextJpa.findById(any())).thenAnswer(i -> Optional.ofNullable(contextStore.get(i.getArgument(0))));
        when(contextJpa.save(any())).thenAnswer(i -> {
            ProjectContextEntity e = i.getArgument(0);
            contextStore.put(e.getProjectId(), e);
            return e;
        });
        contextRepository = new JpaContextRepository(contextJpa, new ObjectMapper());

        project = Project.builder()
                .id(PROJECT_ID)
                .user(User.builder().id(USER_ID).build())
                .title("")
                .description("")
                .category(Category.builder()
                        .code("EMPRESARIAL")
                        .name("Empresarial")
                        .displayOrder(2)
                        .color("#0ea5e9")
                        .active(true)
                        .build())
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenAnswer(i -> Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sync = new ProjectContextSynchronizer(projectRepository);
    }

    private ConversationOrchestrator orchestrator() {
        var pipeline = new Pipeline(List.of(
                new AnalyzerStage(new HeuristicContextAnalyzerAdapter()),
                new EvaluatorStage(new CompletenessEvaluator(EvaluationPolicies.defaults())),
                new StrategistStage(new ConversationStrategist(
                        new DefaultExplorationStrategy(ExplorationPriority.defaultPriorities()))),
                new EventStage()));
        var kinMethod = new KinMethod(pipeline, contextRepository, sync);
        return new ConversationOrchestrator(
                new HistoryWindow(), new DefaultTurnPolicy(), kinMethod, new ResponseGuard(), contextRepository);
    }

    private ConversationTurn turn(String message) {
        return new ConversationTurn(PROJECT_ID, USER_ID, message, List.of(), "Proyecto", "Descripción", "Software");
    }

    private int progress(Project p) {
        var service = new ProjectServiceImpl(
                projectRepository,
                userRepository,
                chatMessageRepository,
                subscriptionValidatorService,
                new CategoryService(categoryRepository));
        try {
            Method m = ProjectServiceImpl.class.getDeclaredMethod("calculateProgress", Project.class);
            m.setAccessible(true);
            return (int) m.invoke(service, p);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void caso1_descubrirNombre_deberiaSincronizarProjectContextYProject() {
        TurnResult result = orchestrator().orchestrate(turn("Mi proyecto se llama Restaurante Vida Sana Caribe"));

        assertEquals("Restaurante Vida Sana Caribe", result.projectContext().value(AnalyzedDimension.PROJECT_NAME));
        assertEquals("Restaurante Vida Sana Caribe", project.getTitle());
        assertFalse(project.getTitle().isBlank());
    }

    @Test
    void caso2_reabrirElProyecto_deberiaDejarDeMostrarCeroPorCiento() {
        orchestrator()
                .orchestrate(
                        turn(
                                "Mi proyecto se llama Restaurante Vida Sana Caribe. El problema es que la gente no come sano."));

        assertEquals("Restaurante Vida Sana Caribe", project.getTitle());
        assertFalse(project.getDescription().isBlank());

        int dashboardProgress = progress(project);
        assertTrue(dashboardProgress > 0, "El Dashboard no debe mostrar 0 % tras la sincronización");
    }

    @Test
    void caso3_conversacionDeberiaRecordarElProyecto() {
        orchestrator().orchestrate(turn("Mi proyecto se llama Restaurante Vida Sana Caribe"));

        var orquestadorReabierto = orchestrator();
        TurnResult reabierto = orquestadorReabierto.orchestrate(turn("contame más"));

        assertEquals("Restaurante Vida Sana Caribe", reabierto.projectContext().value(AnalyzedDimension.PROJECT_NAME));
        assertEquals("Restaurante Vida Sana Caribe", project.getTitle());
    }

    @Test
    void sync_noDebeSobrescribirUnTituloValido() {
        project.setTitle("Título Existente");
        orchestrator().orchestrate(turn("Mi proyecto se llama Restaurante Vida Sana Caribe"));

        assertEquals("Título Existente", project.getTitle());
    }
}





