package com.kinplatform.projectdoc.extract;

/** Error de extracción de texto de un documento (formato no soportado o archivo inválido). */
public class TextExtractionException extends RuntimeException {

    public TextExtractionException(String message) {
        super(message);
    }

    public TextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
