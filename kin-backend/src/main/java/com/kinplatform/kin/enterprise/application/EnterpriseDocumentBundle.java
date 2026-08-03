package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resultado de la exportación de una versión del proyecto empresarial (Fase 10,
 * Milestone 2H).
 *
 * <p>Agrupa las representaciones binarias de todos los documentos de una
 * versión, indexadas por {@link DocumentType} y {@link RenderFormat}: cada
 * documento puede exportarse en uno o varios formatos. Es un valor inmutable:
 * los mapas y los {@code byte[]} se copian de forma defensiva en la
 * construcción, de modo que la instancia no comparte estado mutable con su
 * creador ni con sus consumidores.</p>
 */
public final class EnterpriseDocumentBundle {

    private final UUID projectId;
    private final int version;
    private final Map<DocumentType, Map<RenderFormat, byte[]>> documents;

    private EnterpriseDocumentBundle(UUID projectId, int version,
                                     Map<DocumentType, Map<RenderFormat, byte[]>> documents) {
        this.projectId = projectId;
        this.version = version;
        this.documents = deepCopy(documents);
    }

    /**
     * Crea un bundle a partir de las representaciones por documento y formato.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión del proyecto empresarial exportada
     * @param documents representaciones binarias por documento y formato
     * @return bundle inmutable
     */
    public static EnterpriseDocumentBundle of(UUID projectId, int version,
                                              Map<DocumentType, Map<RenderFormat, byte[]>> documents) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser null");
        }
        if (documents == null) {
            throw new IllegalArgumentException("documents no puede ser null");
        }
        return new EnterpriseDocumentBundle(projectId, version, documents);
    }

    public UUID projectId() {
        return projectId;
    }

    public int version() {
        return version;
    }

    /**
     * Representaciones binarias por documento y formato (inmutable).
     */
    public Map<DocumentType, Map<RenderFormat, byte[]>> documents() {
        return deepCopy(documents);
    }

    /**
     * Representaciones binarias de un documento en todos sus formatos.
     *
     * @param type tipo de documento, o {@code null}
     * @return mapa inmutable por formato (vacío si el documento no existe)
     */
    public Map<RenderFormat, byte[]> renderingsFor(DocumentType type) {
        Map<RenderFormat, byte[]> renderings = documents.get(type);
        if (renderings == null) {
            return Collections.emptyMap();
        }
        EnumMap<RenderFormat, byte[]> copy = new EnumMap<>(RenderFormat.class);
        renderings.forEach((format, bytes) -> copy.put(format, bytes.clone()));
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Representación binaria de un documento en un formato concreto.
     *
     * @param type   tipo de documento, o {@code null}
     * @param format formato de salida, o {@code null}
     * @return los bytes del documento en el formato, o vacío si no existe
     */
    public Optional<byte[]> rendering(DocumentType type, RenderFormat format) {
        Map<RenderFormat, byte[]> renderings = documents.get(type);
        if (renderings == null) {
            return Optional.empty();
        }
        byte[] bytes = renderings.get(format);
        return bytes == null ? Optional.empty() : Optional.of(bytes.clone());
    }

    /**
     * Tipos de documento incluidos en el bundle.
     */
    public Set<DocumentType> types() {
        return documents.keySet();
    }

    /**
     * Número de documentos del bundle.
     */
    public int documentCount() {
        return documents.size();
    }

    /**
     * Número total de representaciones (documentos × formatos).
     */
    public int renderingCount() {
        int count = 0;
        for (Map<RenderFormat, byte[]> renderings : documents.values()) {
            count += renderings.size();
        }
        return count;
    }

    private static Map<DocumentType, Map<RenderFormat, byte[]>> deepCopy(
            Map<DocumentType, Map<RenderFormat, byte[]>> source) {
        EnumMap<DocumentType, Map<RenderFormat, byte[]>> outer = new EnumMap<>(DocumentType.class);
        for (Map.Entry<DocumentType, Map<RenderFormat, byte[]>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("documents no puede contener claves o valores nulos");
            }
            EnumMap<RenderFormat, byte[]> inner = new EnumMap<>(RenderFormat.class);
            for (Map.Entry<RenderFormat, byte[]> rendering : entry.getValue().entrySet()) {
                if (rendering.getKey() == null || rendering.getValue() == null) {
                    throw new IllegalArgumentException("documents no puede contener representaciones nulas");
                }
                inner.put(rendering.getKey(), rendering.getValue().clone());
            }
            outer.put(entry.getKey(), Collections.unmodifiableMap(inner));
        }
        return Collections.unmodifiableMap(outer);
    }
}
