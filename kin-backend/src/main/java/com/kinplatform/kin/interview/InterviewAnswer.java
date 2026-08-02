package com.kinplatform.kin.interview;

/**
 * Respuesta del usuario a una pregunta de entrevista (ADR-015).
 *
 * <p>Value object inmutable: el {@code content} puede estar vacío (la aceptación
 * la decide {@code AnswerValidator} en Java), pero {@code questionId} es
 * obligatorio. La aceptación o el rechazo se representan con
 * {@link AnswerValidation}.</p>
 */
public record InterviewAnswer(
    String questionId,
    String content
) {

    public InterviewAnswer {
        if (questionId == null || questionId.isBlank()) {
            throw new IllegalArgumentException("questionId no puede ser null o vacío");
        }
        content = content == null ? "" : content;
    }

    public static InterviewAnswer of(String questionId, String content) {
        return new InterviewAnswer(questionId, content);
    }

    public boolean hasContent() {
        return !content.isBlank();
    }
}
