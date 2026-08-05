package com.kinplatform.kin.knowledge.planner;

/**
 * Tipo abstracto de proveedor al que apunta una {@link PlannedQuery}
 * (especificación Fase 3). El planner jamás nombra proveedores concretos
 * (DIAN, DANE, Google…): eso pertenece al {@code ProviderRegistry}.
 */
public enum ProviderType {

    GOVERNMENT("Gobierno"),
    STATISTICS("Estadísticas"),
    WEB_SEARCH("Búsqueda web"),
    DOCUMENT("Documentos"),
    VECTOR_RAG("RAG vectorial"),
    INTERNAL_DB("Base interna");

    private final String displayName;

    ProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
