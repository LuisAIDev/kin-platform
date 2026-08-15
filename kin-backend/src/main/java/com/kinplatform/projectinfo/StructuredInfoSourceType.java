package com.kinplatform.projectinfo;

/**
 * Origen de cada dato estructurado del proyecto. KIN distingue si un dato fue
 * proporcionado por el usuario, importado de un documento, calculado, estimado
 * o sugerido por IA; un dato desconocido nunca se presenta como 0.
 */
public enum StructuredInfoSourceType {
    USER_INPUT,
    IMPORTED_DOCUMENT,
    CALCULATED,
    ESTIMATED,
    AI_SUGGESTED
}
