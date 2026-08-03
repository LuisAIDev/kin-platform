package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.ports.DocumentRenderer;
import com.kinplatform.kin.enterprise.renderer.DocxDocumentRenderer;
import com.kinplatform.kin.enterprise.renderer.PdfDocumentRenderer;
import com.kinplatform.kin.enterprise.renderer.PptxDocumentRenderer;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fábrica de renderizadores de documentos (Fase 10, Milestone 2H).
 *
 * <p>Resuelve un {@link DocumentRenderer} a partir de su {@link RenderFormat}
 * mediante un índice inmutable construido en la instanciación (Open/Closed:
 * añadir un formato nuevo solo requiere aportar el renderizador). Rechaza
 * formatos duplicados y dependencias nulas para detectar configuraciones
 * incorrectas en la composición.</p>
 *
 * <p>El constructor por defecto cablea los tres renderizadores del módulo
 * (PDF, DOCX y PPTX); el constructor tipado permite inyectar un conjunto
 * propio de renderizadores. Clase stateless después de la construcción, por lo
 * que es thread-safe.</p>
 */
public final class EnterpriseRendererFactory {

    private final Map<RenderFormat, DocumentRenderer> byFormat;

    /**
     * Cablea los renderizadores por defecto del módulo (PDF, DOCX y PPTX).
     */
    public EnterpriseRendererFactory() {
        this(List.of(new PdfDocumentRenderer(), new DocxDocumentRenderer(), new PptxDocumentRenderer()));
    }

    /**
     * @param renderers renderizadores disponibles (obligatorio, sin nulos y sin
     *                  formatos duplicados)
     * @throws IllegalArgumentException si {@code renderers} es {@code null},
     *                                  contiene nulos o formatos duplicados
     */
    public EnterpriseRendererFactory(List<DocumentRenderer> renderers) {
        if (renderers == null) {
            throw new IllegalArgumentException("renderers no puede ser null");
        }
        EnumMap<RenderFormat, DocumentRenderer> index = new EnumMap<>(RenderFormat.class);
        for (DocumentRenderer renderer : renderers) {
            if (renderer == null) {
                throw new IllegalArgumentException("renderers no puede contener nulos");
            }
            RenderFormat format = renderer.format();
            if (format == null) {
                throw new IllegalArgumentException("Un renderizador debe declarar su formato");
            }
            if (index.putIfAbsent(format, renderer) != null) {
                throw new IllegalArgumentException("Formato duplicado en la fábrica: " + format);
            }
        }
        this.byFormat = Collections.unmodifiableMap(index);
    }

    /**
     * Devuelve el renderizador del formato solicitado.
     *
     * @param format formato de salida (obligatorio)
     * @return el renderizador registrado para el formato
     * @throws IllegalArgumentException si {@code format} es {@code null} o no
     *                                  hay renderizador registrado
     */
    public DocumentRenderer rendererFor(RenderFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("format no puede ser null");
        }
        DocumentRenderer renderer = byFormat.get(format);
        if (renderer == null) {
            throw new IllegalArgumentException("No hay renderizador registrado para el formato " + format);
        }
        return renderer;
    }

    /**
     * Indica si hay un renderizador registrado para el formato dado.
     *
     * @param format formato de salida, o {@code null}
     * @return {@code true} si el formato es soportado
     */
    public boolean supports(RenderFormat format) {
        return format != null && byFormat.containsKey(format);
    }

    /**
     * Formatos de salida soportados, en orden de declaración del enum.
     *
     * @return conjunto inmutable de formatos soportados
     */
    public Set<RenderFormat> supportedFormats() {
        return Collections.unmodifiableSet(EnumSet.copyOf(byFormat.keySet()));
    }
}
