package com.kinplatform.billing.rips.validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.rips.RipsSerializationException;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Convierte la linea RIPS (JSON en {@code ripsLineData}) a XML para validacion XSD.
 *
 * <p><b>PLACEHOLDER:</b> los nombres de elementos y el orden siguen una
 * aproximacion al Anexo Tecnico MinSalud. Cuando el PO entregue los XSD
 * oficiales (TD-CAT-4) se ajustan los mapeos por tipo.</p>
 */
@Component
@RequiredArgsConstructor
public class RipsJsonToXmlMapper {

    private final ObjectMapper objectMapper;

    public String toXml(RipsRecord record, RipsBatch.RipsType type) {
        try {
            Map<String, Object> data = objectMapper.readValue(
                    record.getRipsLineData(), new TypeReference<Map<String, Object>>() {});
            String root = type.name().toLowerCase();
            StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xml.append("<rips><").append(root).append(">");
            for (Map.Entry<String, Object> field : fieldsFor(type, data)) {
                if (field.getValue() == null) {
                    continue;
                }
                xml.append('<').append(field.getKey()).append('>')
                        .append(escape(String.valueOf(field.getValue())))
                        .append("</").append(field.getKey()).append('>');
            }
            xml.append("</").append(root).append("></rips>");
            return xml.toString();
        } catch (Exception e) {
            throw new RipsSerializationException("Error JSON->XML: " + e.getMessage(), e);
        }
    }

    private List<Map.Entry<String, Object>> fieldsFor(RipsBatch.RipsType type, Map<String, Object> data) {
        return switch (type) {
            case AC -> build(data,
                    "codPrestador", "codigoPrestador",
                    "numFactura", "numFactura",
                    "codConsulta", "codigoConsulta",
                    "tipoDocumento", "tipoDocumento",
                    "numDocumento", "numDocumento",
                    "fechaConsulta", "fechaConsulta",
                    "codDiagnosticoPrincipal", "codigoDiagnosticoPrincipal",
                    "valorConsulta", "valorConsulta");
            case AP -> build(data,
                    "codPrestador", "codigoPrestador",
                    "numFactura", "numFactura",
                    "codProcedimiento", "codigoProcedimiento",
                    "fechaProcedimiento", "fechaProcedimiento",
                    "codDiagnosticoPrincipal", "codigoDiagnostico",
                    "valorProcedimiento", "valorProcedimiento");
            case AU -> build(data,
                    "codPrestador", "codigoPrestador",
                    "numFactura", "numFactura",
                    "fechaUrgencia", "fechaUrgencia",
                    "motivoUrgencia", "motivoUrgencia",
                    "codDiagnosticoSalida", "codigoDiagnosticoSalida",
                    "valorUrgencia", "valorUrgencia");
            case AT -> build(data,
                    "codPrestador", "codigoPrestador",
                    "numFactura", "numFactura",
                    "tipoOtrosServicios", "tipoOtrosServicios",
                    "codServicio", "codigoServicio",
                    "cantidad", "cantidad",
                    "valorUnitario", "valorUnitario",
                    "valorTotal", "valorTotal",
                    "fechaServicio", "fechaServicio");
            case US -> build(data,
                    "tipoDocumento", "tipoDocumento",
                    "numDocumento", "numeroDocumento",
                    "primerNombre", "primerNombre",
                    "segundoNombre", "segundoNombre",
                    "primerApellido", "primerApellido",
                    "segundoApellido", "segundoApellido",
                    "fechaNacimiento", "fechaNacimiento",
                    "sexo", "sexo");
            case AF -> build(data,
                    "codPrestador", "codigoPrestador",
                    "nitEps", "nitEps",
                    "periodoInicio", "periodoInicio",
                    "periodoFin", "periodoFin",
                    "numeroFactura", "numeroFactura",
                    "valorTotal", "valorTotal",
                    "tipoFactura", "tipoFactura");
        };
    }

    private List<Map.Entry<String, Object>> build(Map<String, Object> data, String... pairs) {
        List<Map.Entry<String, Object>> fields = new ArrayList<>(pairs.length / 2);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String element = pairs[i];
            String key = pairs[i + 1];
            fields.add(new java.util.AbstractMap.SimpleEntry<>(element, data.get(key)));
        }
        return fields;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
