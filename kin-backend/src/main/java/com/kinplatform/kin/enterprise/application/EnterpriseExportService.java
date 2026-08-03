package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.util.EnumMap;
import java.util.Map;

/**
 * Servicio de exportación de documentos del proyecto empresarial (Fase 10,
 * Milestone 2H).
 *
 * <p>Genera las representaciones binarias de los documentos de una versión del
 * proyecto ({@link EnterpriseProject}) en todos los formatos soportados por la
 * {@link EnterpriseRendererFactory}: cada documento del aggregate se renderiza
 * con el {@code DocumentRenderer} correspondiente a cada formato y el resultado
 * se agrupa en un {@link EnterpriseDocumentBundle} inmutable.</p>
 *
 * <p>Exposición escalonada: {@link #export(EnterpriseProject)} exporta todos
 * los documentos en todos los formatos; {@link #export(EnterpriseProject,
 * DocumentType)} exporta un documento en todos los formatos y
 * {@link #export(EnterpriseProject, DocumentType, RenderFormat)} exporta un
 * documento en un formato concreto. Los formatos sin renderizador registrado se
 * omiten en la exportación completa (modo tolerante). Servicio stateless y
 * thread-safe: no conserva estado y solo transforma datos.</p>
 */
public final class EnterpriseExportService {

    private final EnterpriseRendererFactory rendererFactory;

    /**
     * @param rendererFactory fábrica de renderizadores (obligatoria)
     */
    public EnterpriseExportService(EnterpriseRendererFactory rendererFactory) {
        if (rendererFactory == null) {
            throw new IllegalArgumentException("rendererFactory no puede ser null");
        }
        this.rendererFactory = rendererFactory;
    }

    /**
     * Exporta todos los documentos de la versión en todos los formatos
     * soportados.
     *
     * @param project proyecto empresarial (una versión), obligatorio
     * @return bundle inmutable con las representaciones binarias
     * @throws IllegalArgumentException si {@code project} es {@code null}
     */
    public EnterpriseDocumentBundle export(EnterpriseProject project) {
        if (project == null) {
            throw new IllegalArgumentException("project no puede ser null");
        }
        EnumMap<DocumentType, Map<RenderFormat, byte[]>> renderings =
            new EnumMap<>(DocumentType.class);
        for (DocumentArtifact artifact : project.documents()) {
            renderings.put(artifact.type(), renderAllFormats(artifact));
        }
        return EnterpriseDocumentBundle.of(project.projectId(), project.version(), renderings);
    }

    /**
     * Exporta un documento concreto de la versión en todos los formatos
     * soportados.
     *
     * @param project proyecto empresarial (obligatorio)
     * @param type    tipo de documento a exportar (obligatorio)
     * @return mapa inmutable con la representación binaria por formato
     * @throws EnterpriseExportException   si la versión no contiene el documento
     * @throws IllegalArgumentException    si {@code project} o {@code type} son
     *                                     {@code null}
     */
    public Map<RenderFormat, byte[]> export(EnterpriseProject project, DocumentType type) {
        return renderAllFormats(requireDocument(project, type));
    }

    /**
     * Exporta un documento concreto de la versión en un formato específico.
     *
     * @param project proyecto empresarial (obligatorio)
     * @param type    tipo de documento a exportar (obligatorio)
     * @param format  formato de salida (obligatorio)
     * @return bytes del documento renderizado en el formato solicitado
     * @throws EnterpriseExportException   si la versión no contiene el documento
     * @throws IllegalArgumentException    si {@code project}, {@code type} o
     *                                     {@code format} son {@code null}, o si
     *                                     no hay renderizador para el formato
     */
    public byte[] export(EnterpriseProject project, DocumentType type, RenderFormat format) {
        DocumentArtifact artifact = requireDocument(project, type);
        if (format == null) {
            throw new IllegalArgumentException("format no puede ser null");
        }
        return rendererFactory.rendererFor(format).render(artifact);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private Map<RenderFormat, byte[]> renderAllFormats(DocumentArtifact artifact) {
        EnumMap<RenderFormat, byte[]> formats = new EnumMap<>(RenderFormat.class);
        for (RenderFormat format : rendererFactory.supportedFormats()) {
            formats.put(format, rendererFactory.rendererFor(format).render(artifact));
        }
        return Map.copyOf(formats);
    }

    private DocumentArtifact requireDocument(EnterpriseProject project, DocumentType type) {
        if (project == null) {
            throw new IllegalArgumentException("project no puede ser null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type no puede ser null");
        }
        return project.findDocument(type).orElseThrow(
            () -> new EnterpriseExportException(
                "La versión del proyecto no contiene un documento de tipo " + type + "."));
    }
}
