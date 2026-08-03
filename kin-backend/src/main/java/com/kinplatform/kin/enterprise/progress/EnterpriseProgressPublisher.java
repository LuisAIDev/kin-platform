package com.kinplatform.kin.enterprise.progress;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;

/**
 * Publicador de progreso de la generación (Fase 10, Milestone 2J).
 *
 * <p>Traduce el estado de una versión del aggregate {@link EnterpriseProject}
 * a la secuencia de {@link EnterpriseProgressEvent} correspondiente y la
 * publica en el {@link EnterpriseProgressSink}: {@code REQUESTED} y
 * {@code RUNNING} para los estados en vuelo, {@code DOCUMENT_GENERATED} por
 * cada documento, y {@code COMPLETED} o {@code FAILED} como cierre. Lo usan
 * tanto el decorador del repositorio (publica el progreso de cada
 * {@code save}) como el controlador SSE (publica el estado inicial al
 * suscribirse un cliente). Clase stateless y thread-safe.</p>
 */
public final class EnterpriseProgressPublisher {

    private final EnterpriseProgressSink sink;

    /**
     * @param sink puerto de publicación de progreso (obligatorio)
     */
    public EnterpriseProgressPublisher(EnterpriseProgressSink sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink no puede ser null");
        }
        this.sink = sink;
    }

    /**
     * Publica los eventos de progreso correspondientes al estado actual de la
     * versión.
     *
     * @param project versión del proyecto empresarial (obligatoria)
     */
    public void publishFor(EnterpriseProject project) {
        if (project == null) {
            throw new IllegalArgumentException("project no puede ser null");
        }
        switch (project.status()) {
            case REQUESTED -> sink.publish(EnterpriseProgressEvent.of(
                project.projectId(), project.version(), EnterpriseProgressState.REQUESTED,
                null, "Generación solicitada"));
            case RUNNING -> sink.publish(EnterpriseProgressEvent.of(
                project.projectId(), project.version(), EnterpriseProgressState.RUNNING,
                null, "Generación en curso"));
            case COMPLETED -> {
                publishDocuments(project);
                sink.publish(EnterpriseProgressEvent.of(
                    project.projectId(), project.version(), EnterpriseProgressState.COMPLETED,
                    null, "Generación completada"));
            }
            case FAILED -> {
                publishDocuments(project);
                sink.publish(EnterpriseProgressEvent.of(
                    project.projectId(), project.version(), EnterpriseProgressState.FAILED,
                    null, project.failedReason() != null
                        ? "Generación fallida: " + project.failedReason()
                        : "Generación fallida"));
            }
        }
    }

    private void publishDocuments(EnterpriseProject project) {
        for (DocumentArtifact document : project.documents()) {
            sink.publish(EnterpriseProgressEvent.of(
                project.projectId(), project.version(), EnterpriseProgressState.DOCUMENT_GENERATED,
                document.type(), "Documento " + document.type() + " generado"));
        }
    }
}
