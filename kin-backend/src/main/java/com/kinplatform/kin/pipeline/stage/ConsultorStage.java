package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

/**
 * Etapa de consultoría: pide la respuesta de IA al puerto {@link AIResponder}.
 *
 * <p>Depende del puerto de dominio (no del {@code AiEngineService} concreto),
 * por lo que respeta la dirección de dependencias de la arquitectura limpia.
 * La construcción del prompt la delega al {@link PromptAssembler}.</p>
 *
 * <p>En modo streaming no bloquea: deja el {@code Flux} en el contexto para que
 * {@code KinMethod.executeStream} lo devuelva al orquestador SSE.</p>
 */
public class ConsultorStage implements PipelineStage {

    private final AIResponder aiResponder;
    private final PromptAssembler promptAssembler;

    public ConsultorStage(AIResponder aiResponder, PromptAssembler promptAssembler) {
        this.aiResponder = aiResponder;
        this.promptAssembler = promptAssembler;
    }

    @Override
    public String name() {
        return "Consultor";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return true;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var systemPrompt = promptAssembler.assemble(
            context.projectTitle(),
            context.projectDescription(),
            context.projectCategory(),
            context.projectContext()
        );
        var request = new AIRequest(context.history(), context.userMessage(), systemPrompt);
        if (context.streaming()) {
            context.aiResponseFlux(aiResponder.respondStream(request));
        } else {
            context.aiResponse(aiResponder.respond(request));
        }
        return context;
    }
}
