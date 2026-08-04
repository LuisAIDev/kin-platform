package com.kinplatform.common.config;

import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.KinMethodCommand;
import com.kinplatform.kin.KinMethodResult;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.ConversationTurn;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de wiring del ciclo automático Enterprise (Fase 10, M3B): verifica que
 * {@link KinConfig#conversationOrchestrator} inyecta el
 * {@link EnterpriseProjectTrigger} real (y no el {@code NO_OP_TRIGGER}) al
 * {@link ConversationOrchestrator} usando el constructor aditivo de 6 argumentos.
 */
@ExtendWith(MockitoExtension.class)
class KinConfigConversationOrchestratorWiringTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private final KinConfig kinConfig = new KinConfig();

    @Mock
    private KinMethod kinMethod;

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private EnterpriseProjectTrigger enterpriseProjectTrigger;

    @Test
    void conversationOrchestrator_inyectaElTriggerReal_enLugarDeNoOp() {
        var contexto = contextoReporte();
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", "Software"))
            .thenReturn(contexto);
        when(kinMethod.execute(any(KinMethodCommand.class))).thenReturn(new KinMethodResult(
            contexto, null, ConversationDecision.generateReport("informe"),
            "Aquí tenés el informe de viabilidad completo.", null, List.of(), ConsultingReport.empty()));

        ConversationOrchestrator orchestrator = kinConfig.conversationOrchestrator(
            new HistoryWindow(), new DefaultTurnPolicy(), kinMethod,
            new ResponseGuard(), contextRepository, enterpriseProjectTrigger);

        var result = orchestrator.orchestrate(turn());

        assertNotNull(result);
        verify(enterpriseProjectTrigger).request(PROJECT_ID);
    }

    private ProjectContext contextoReporte() {
        var contexto = ProjectContext.fromProject("Proyecto Test", "Descripción", "Software");
        contexto.markReportGenerated();
        contexto.attachDecision(ConversationDecision.generateReport("informe"));
        return contexto;
    }

    private ConversationTurn turn() {
        return new ConversationTurn(PROJECT_ID, USER_ID, "Generá el informe", List.of(),
            "Proyecto Test", "Descripción", "Software");
    }
}
