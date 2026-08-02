package com.kinplatform.kin.interview.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineMetadata;
import com.kinplatform.kin.engine.EnginePhase;
import com.kinplatform.kin.engine.EngineType;
import com.kinplatform.kin.interview.AnswerValidation;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewDecision;
import com.kinplatform.kin.interview.InterviewDirective;
import com.kinplatform.kin.interview.InterviewInput;
import com.kinplatform.kin.interview.InterviewRequest;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.interview.InterviewState;

import java.util.HashMap;
import java.util.List;

/**
 * Motor canonizado de la entrevista estratégica (ADR-015, Etapa E3; ADR-005/009).
 *
 * <p>Implementa {@link DomainEngine} y decide en Java, de forma determinista y
 * sin efectos secundarios: qué información falta, cuál es la siguiente
 * pregunta, cuándo la entrevista está completa y cuándo generar el
 * {@link InterviewResult}. Delega el plan en {@link InterviewBlueprint} y la
 * validación de respuestas en {@link AnswerValidator}.</p>
 *
 * <p>El estado viaja en el {@link InterviewInput} (estado previo + respuesta
 * del turno) y el resultado contiene el nuevo {@link InterviewState}; el motor
 * nunca persiste (eso es responsabilidad de la etapa de pipeline, Etapa E5) y
 * nunca genera prompts, llama a un LLM ni toca la red. Si la entrada no es
 * procesable, degrada a {@link InterviewResult#empty()} sin lanzar.</p>
 *
 * <p>Nota de contrato: ADR-015 sanciona el valor aditivo {@code INTERVIEW} de
 * {@link EnginePhase} en la Etapa E5; mientras tanto se declara la fase
 * {@code VALIDATION} para no modificar el contrato congelado {@code kin/engine}.</p>
 */
public class InterviewEngine implements DomainEngine<InterviewInput, InterviewResult> {

    public static final String GENERATOR_NAME = "InterviewEngine";
    public static final String ENGINE_VERSION = "v1";

    private final InterviewBlueprint blueprint;
    private final AnswerValidator validator;

    /**
     * @param blueprint plan determinista de la entrevista
     * @param validator validador determinista de respuestas
     */
    public InterviewEngine(InterviewBlueprint blueprint, AnswerValidator validator) {
        this.blueprint = blueprint;
        this.validator = validator;
    }

    @Override
    public EngineMetadata metadata() {
        return EngineMetadata.of(GENERATOR_NAME, ENGINE_VERSION, "KIN Architecture Team",
            EnginePhase.VALIDATION, EngineType.DOMAIN, 40);
    }

    @Override
    public InterviewResult evaluate(InterviewInput input) {
        if (input == null || input.request() == null || blueprint == null || validator == null) {
            return InterviewResult.empty();
        }
        InterviewRequest request = input.request();
        InterviewState state = request.previousState();
        if (state == null || state.isComplete()) {
            return report(state);
        }
        InterviewState afterAnswer = processAnswer(request.answer(), state);
        if (blueprint.isComplete(afterAnswer)) {
            return report(afterAnswer);
        }
        var next = blueprint.next(afterAnswer);
        if (next.isEmpty()) {
            return report(afterAnswer);
        }
        var question = next.get();
        var asked = afterAnswer
            .withCurrent(question.id())
            .withPending(blueprint.pendingIds(afterAnswer))
            .withExchangeUsed(afterAnswer.exchangeUsed() + 1)
            .withComplete(false);
        var directive = InterviewDirective.of(question.id(), question.dimension(),
            question.topic(), question.rules());
        var decision = InterviewDecision.ask(question.id(), "Falta información: " + question.topic());
        return build(decision, directive, asked);
    }

    private InterviewState processAnswer(InterviewAnswer answer, InterviewState state) {
        if (answer == null || state.current() == null) {
            return state;
        }
        if (!state.current().equals(answer.questionId())) {
            return state;
        }
        var question = blueprint.question(answer.questionId()).orElse(null);
        if (question == null) {
            return state;
        }
        int refinements = state.refinements().getOrDefault(state.current(), 0);
        AnswerValidation validation = validator.validate(answer.content(), question.rules(), refinements);
        if (validation.isAccepted()) {
            var answered = new HashMap<>(state.answered());
            answered.put(state.current(), answer);
            var refinementsMap = new HashMap<>(state.refinements());
            refinementsMap.remove(state.current());
            return state.withAnswered(answered).withRefinements(refinementsMap).withCurrent(null);
        }
        if (validation.requiresRefinement()) {
            var refinementsMap = new HashMap<>(state.refinements());
            refinementsMap.put(state.current(), validation.refinementCount());
            return state.withRefinements(refinementsMap);
        }
        return state;
    }

    private InterviewResult report(InterviewState state) {
        if (state == null) {
            return InterviewResult.empty();
        }
        InterviewState completed = state.withComplete(true).withCurrent(null).withPending(List.of());
        return build(InterviewDecision.report("Entrevista completa."), null, completed);
    }

    private InterviewResult build(InterviewDecision decision, InterviewDirective directive, InterviewState state) {
        if (state == null) {
            return InterviewResult.empty();
        }
        var progress = state.toProgress(blueprint.totalQuestions());
        var explanation = decision.isReport()
            ? "Entrevista completa. " + progress.answeredCount() + " respuestas de "
                + blueprint.totalQuestions() + " preguntas."
            : "Falta información: " + (directive == null ? "" : directive.topic()) + ".";
        double confidence = progress.completenessRatio();
        return new InterviewResult(decision, directive, state, progress, confidence,
            explanation, GENERATOR_NAME, ENGINE_VERSION);
    }
}
