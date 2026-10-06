package com.kinplatform.kin.enterprise.integration;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.projectinfo.StructuredInfoSourceType;
import java.util.List;

/**
 * Mapeo entre las 14 dimensiones de {@code ProjectContext} y las claves de
 * {@code project_structured_info}. Es una vista de enriquecimiento: no muta el
 * contexto.
 */
final class DimensionSourceMapping {

    private DimensionSourceMapping() {}

    /** Prioridad de origen: USER_INPUT es la fuente de mayor confianza. */
    static final List<StructuredInfoSourceType> SOURCE_PRIORITY = List.of(
            StructuredInfoSourceType.USER_INPUT,
            StructuredInfoSourceType.IMPORTED_DOCUMENT,
            StructuredInfoSourceType.CALCULATED,
            StructuredInfoSourceType.ESTIMATED,
            StructuredInfoSourceType.AI_SUGGESTED);

    /**
     * Claves candidatas por dimensión. Se evalúan en orden; la primera con
     * valor resuelto gana según la prioridad de origen.
     */
    static List<DimensionKey> candidateKeys(AnalyzedDimension dimension) {
        return switch (dimension) {
            case PROJECT_NAME -> keys("DATOS_GENERALES", "nombre");
            case SECTOR -> keys("DATOS_GENERALES", "sector");
            case CITY -> keys("DATOS_GENERALES", "ubicacion");
            case PROBLEM -> keys("DATOS_GENERALES", "problema");
            case SOLUTION -> keys("DATOS_GENERALES", "descripcion", "MODELO_DE_NEGOCIO", "producto_servicio");
            case TARGET_CUSTOMER -> keys("DATOS_GENERALES", "publico_objetivo", "MODELO_DE_NEGOCIO", "clientes");
            case VALUE_PROPOSITION -> keys("MODELO_DE_NEGOCIO", "propuesta_de_valor");
            case REVENUE_MODEL -> keys("MODELO_DE_NEGOCIO", "fuentes_de_ingresos");
            case COMPETITION -> keys("MERCADO", "competidores");
            case RISKS -> keys("RIESGOS", "riesgos_identificados");
            case RESOURCES -> keys("OPERACION", "empleados", "OPERACION", "equipos");
            case SCALABILITY -> keys("OPERACION", "capacidad_produccion");
            case MVP, OBJECTIVES -> List.of();
        };
    }

    private static List<DimensionKey> keys(String sectionA, String keyA) {
        return List.of(new DimensionKey(sectionA, keyA));
    }

    private static List<DimensionKey> keys(String sectionA, String keyA, String sectionB, String keyB) {
        return List.of(new DimensionKey(sectionA, keyA), new DimensionKey(sectionB, keyB));
    }

    record DimensionKey(String section, String key) {}
}

