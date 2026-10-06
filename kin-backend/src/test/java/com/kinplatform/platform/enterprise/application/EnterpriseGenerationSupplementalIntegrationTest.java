package com.kinplatform.platform.enterprise.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.kinplatform.platform.enterprise.assembler.EnterpriseDocumentAssembler;
import com.kinplatform.platform.enterprise.engine.DefaultBusinessModelEngine;
import com.kinplatform.platform.enterprise.engine.DefaultEnterpriseScoreEngine;
import com.kinplatform.platform.enterprise.engine.DefaultFinancialPlanEngine;
import com.kinplatform.platform.enterprise.engine.DefaultInnovationEngine;
import com.kinplatform.platform.enterprise.engine.DefaultKpiEngine;
import com.kinplatform.platform.enterprise.engine.DefaultMarketEngine;
import com.kinplatform.platform.enterprise.engine.DefaultRiskPlanEngine;
import com.kinplatform.platform.enterprise.engine.DefaultRoadmapEngine;
import com.kinplatform.platform.enterprise.engine.EngineTestFixtures;
import com.kinplatform.platform.enterprise.engine.FinancialPlanEngine;
import com.kinplatform.platform.enterprise.engine.input.FinancialPlanInput;
import com.kinplatform.platform.enterprise.integration.EnterpriseSupplementalInput;
import com.kinplatform.platform.enterprise.integration.ResolvedValue;
import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifica que {@code EnterpriseGenerationService} enriquece el plan de mercado
 * con los datos numéricos estructurados (FASE 10B) y que con entrada vacía se
 * comporta como identidad.
 */
class EnterpriseGenerationSupplementalIntegrationTest {

    private InMemoryEnterpriseProjectRepository repository;
    private InMemoryDomainEventBus eventBus;
    private final AtomicReference<FinancialPlanInput> captured = new AtomicReference<>();

    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new InMemoryEnterpriseProjectRepository();
        eventBus = new InMemoryDomainEventBus();
    }

    private EnterpriseGenerationService service(FinancialPlanEngine financialPlanEngine) {
        return new EnterpriseGenerationService(
                new DefaultBusinessModelEngine(),
                new DefaultMarketEngine(),
                new DefaultInnovationEngine(),
                financialPlanEngine,
                new DefaultRoadmapEngine(),
                new DefaultRiskPlanEngine(),
                new DefaultKpiEngine(),
                new DefaultEnterpriseScoreEngine(),
                new EnterpriseDocumentAssembler(),
                repository,
                eventBus,
                ForkJoinPool.commonPool());
    }

    private EnterpriseGenerationRequest request() {
        return new EnterpriseGenerationRequest(projectId, EngineTestFixtures.contextWithAll(), null, null, null, null);
    }

    @Test
    void conSuplementoVacioElPlanDeMercadoNoSeModifica() {
        FinancialPlanEngine financial = capturingSpy();
        EnterpriseGenerationService svc = service(financial);

        svc.generateWithSupplemental(request(), EnterpriseSupplementalInput.empty());

        assertThat(captured.get().marketPlan().som()).isZero();
    }

    @Test
    void conDatosEstructuradosElPlanDeMercadoSeEnriquece() {
        FinancialPlanEngine financial = capturingSpy();
        EnterpriseGenerationService svc = service(financial);
        EnterpriseSupplementalInput supplemental = new EnterpriseSupplementalInput(
                ResolvedValue.of("4500000000", StructuredInfoSourceType.IMPORTED_DOCUMENT, "Documento"),
                ResolvedValue.of("1800000000", StructuredInfoSourceType.USER_INPUT, "Usuario"),
                ResolvedValue.of("900000000", StructuredInfoSourceType.USER_INPUT, "Usuario"),
                ResolvedValue.of("12", StructuredInfoSourceType.ESTIMATED, "Estimado"),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending(),
                ResolvedValue.pending());

        svc.generateWithSupplemental(request(), supplemental);

        assertThat(captured.get().marketPlan().som()).isEqualTo(900_000_000.0);
        assertThat(captured.get().marketPlan().tam()).isEqualTo(4_500_000_000.0);
        assertThat(captured.get().marketPlan().growthRate()).isEqualTo(12.0);
    }

    private FinancialPlanEngine capturingSpy() {
        FinancialPlanEngine engine = spy(new DefaultFinancialPlanEngine());
        doAnswer(invocation -> {
                    captured.set(invocation.getArgument(0));
                    return invocation.callRealMethod();
                })
                .when(engine)
                .evaluate(any(FinancialPlanInput.class));
        return engine;
    }
}




