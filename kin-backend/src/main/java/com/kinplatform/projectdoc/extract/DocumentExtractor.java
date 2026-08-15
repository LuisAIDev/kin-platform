package com.kinplatform.projectdoc.extract;

import org.springframework.web.multipart.MultipartFile;

/**
 * Extrae texto plano de un documento importado. Cada implementación soporta un
 * conjunto de formatos y valida tanto el tipo MIME como la extensión del
 * archivo (nunca se confía únicamente en el Content-Type declarado por el
 * navegador).
 */
public interface DocumentExtractor {

    boolean supports(String mimeType, String filename);

    String extract(MultipartFile file) throws Exception;
}
