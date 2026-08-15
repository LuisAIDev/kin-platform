package com.kinplatform.projectdoc.extract;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Coordina los extractores de texto disponibles. Selecciona el primer
 * {@link DocumentExtractor} que soporta el tipo MIME/extensión del archivo; si
 * ninguno lo soporta o la extracción falla, lanza {@link TextExtractionException}.
 */
@Service
public class DocumentExtractionService {

    private final List<DocumentExtractor> extractors;

    public DocumentExtractionService(List<DocumentExtractor> extractors) {
        this.extractors = extractors;
    }

    public String extract(MultipartFile file, String mimeType) {
        return extractors.stream()
                .filter(e -> e.supports(mimeType, file.getOriginalFilename()))
                .findFirst()
                .map(e -> {
                    try {
                        return e.extract(file);
                    } catch (Exception ex) {
                        throw new TextExtractionException("No se pudo extraer el texto del documento", ex);
                    }
                })
                .orElseThrow(() -> new TextExtractionException("Formato de documento no soportado: " + mimeType));
    }
}
