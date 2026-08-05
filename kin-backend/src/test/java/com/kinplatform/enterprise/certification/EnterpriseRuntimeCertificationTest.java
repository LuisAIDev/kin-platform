package com.kinplatform.enterprise.certification;

import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.knowledge.engine.KnowledgeEngine;
import com.kinplatform.kin.knowledge.engine.KnowledgeGateway;
import com.kinplatform.kin.knowledge.stage.KnowledgeStage;
import com.kinplatform.kin.pipeline.Pipeline;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Certificación del runtime (Fase 9): arranca el contexto Spring completo bajo el
 * perfil {@code test} y verifica que los beans del Knowledge Engine y del pipeline
 * se inicializan correctamente, sin dependencias circulares ni configuraciones
 * huérfanas (Spring falla en el arranque ante ciclos).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
        "springdotenv.enabled=false"
    })
@ActiveProfiles("test")
class EnterpriseRuntimeCertificationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextoDeberiaInicializarLosBeansDelKnowledgeEngine() {
        assertNotNull(context.getBean(KnowledgeEngine.class));
        assertNotNull(context.getBean(KnowledgeGateway.class));
        assertNotNull(context.getBean(KnowledgeStage.class));
    }

    @Test
    void contextoDeberiaInicializarPipelineYOrquestacion() {
        assertNotNull(context.getBean(Pipeline.class));
        assertNotNull(context.getBean(KinMethod.class));
        assertNotNull(context.getBean(ConversationOrchestrator.class));
        assertNotNull(context.getBean(PromptAssembler.class));
    }

    @Test
    void contextoDeberiaInicializarObservabilidadYSeguridad() {
        assertNotNull(context.getBean(MeterRegistry.class));
    }
}
