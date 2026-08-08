package com.kinplatform.project;

/**
 * Lanzada cuando no existe un reporte de consultoría para el proyecto (o el
 * proyecto no pertenece al usuario autenticado). Se mapea a HTTP 404 para no
 * filtrar la existencia de proyectos ajenos.
 */
public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String message) {
        super(message);
    }
}
