package com.kinplatform.kin.medical.billing.rips.ubl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UblExtensionFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static RipsInteroperabilidadPayload fromRecord(RipsRecord record, String codigoPrestador) {
        CustomTagGeneral responsable = CustomTagGeneral.builder()
                .name("Responsable")
                .value("url www.minsalud.gov.co")
                .build();

        CustomTagGeneral actoAdministrativo = CustomTagGeneral.builder()
                .name("Tipo, identificacion del acto administrativo")
                .value("Resolucion 2275:2023")
                .build();

        Map<String, Object> data = parseRipsLineData(record.getRipsLineData());

        List<AdditionalInformation> additionalInfos = new ArrayList<>();

        addIfPresent(additionalInfos, "CODIGO_PRESTADOR", codigoPrestador, "salud codigo_habilitacion");
        addIfPresent(additionalInfos, data, "tipoOperacion", "TIPO_OPERACION", null);
        addIfPresent(additionalInfos, data, "modalidadPago", "MODALIDAD_PAGO", "salud modalidad_pago");
        addIfPresent(additionalInfos, data, "coberturaPlanBeneficios", "COBERTURA_PLAN_BENEFICIOS", "salud cobertura_plan");
        addIfPresent(additionalInfos, data, "numeroContrato", "NUMERO_CONTRATO", null);
        addIfPresent(additionalInfos, data, "numeroPoliza", "NUMERO_POLIZA", null);
        addIfPresentBigDecimal(additionalInfos, data, "copago", "COPAGO");
        addIfPresentBigDecimal(additionalInfos, data, "cuotaModeradora", "CUOTA_MODERADORA");
        addIfPresentBigDecimal(additionalInfos, data, "pagosCompartidos", "PAGOS_COMPARTIDOS");
        addIfPresentDate(additionalInfos, data, "fechaInicioPeriodo", "FECHA_INICIO_PERIODO");
        addIfPresentDate(additionalInfos, data, "fechaFinPeriodo", "FECHA_FIN_PERIODO");

        UsuarioCollection usuarioCollection = UsuarioCollection.builder()
                .schemeName("Usuario")
                .additionalInformations(additionalInfos)
                .build();

        InteroperabilidadGroup group = InteroperabilidadGroup.builder()
                .schemeName("Sector Salud")
                .collections(List.of(usuarioCollection))
                .build();

        Interoperabilidad interoperabilidad = Interoperabilidad.builder()
                .group(group)
                .build();

        UblExtensionContent extensionContent = UblExtensionContent.builder()
                .customTagGenerals(List.of(responsable, actoAdministrativo))
                .interoperabilidad(interoperabilidad)
                .build();

        UblExtension extension = UblExtension.builder()
                .extensionContent(extensionContent)
                .build();

        return RipsInteroperabilidadPayload.builder()
                .extensions(List.of(extension))
                .build();
    }

    private static Map<String, Object> parseRipsLineData(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static void addIfPresent(List<AdditionalInformation> list, Map<String, Object> data, String key, String name, String schemeName) {
        Object value = data.get(key);
        if (value != null) {
            String strValue = value.toString().trim();
            if (!strValue.isEmpty()) {
                list.add(AdditionalInformation.builder()
                        .name(name)
                        .value(strValue)
                        .valueSchemeName(schemeName)
                        .build());
            }
        }
    }

    private static void addIfPresent(List<AdditionalInformation> list, String name, String value, String schemeName) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(AdditionalInformation.builder()
                    .name(name)
                    .value(value.trim())
                    .valueSchemeName(schemeName)
                    .build());
        }
    }

    private static void addIfPresentBigDecimal(List<AdditionalInformation> list, Map<String, Object> data, String key, String name) {
        Object value = data.get(key);
        if (value != null) {
            String strValue;
            if (value instanceof BigDecimal bd) {
                strValue = bd.stripTrailingZeros().toPlainString();
            } else {
                strValue = value.toString().trim();
            }
            if (!strValue.isEmpty()) {
                list.add(AdditionalInformation.builder()
                        .name(name)
                        .value(strValue)
                        .valueSchemeName(null)
                        .build());
            }
        }
    }

    private static void addIfPresentDate(List<AdditionalInformation> list, Map<String, Object> data, String key, String name) {
        Object value = data.get(key);
        if (value != null) {
            String strValue;
            if (value instanceof LocalDate ld) {
                strValue = ld.format(DATE_FORMAT);
            } else {
                strValue = value.toString().trim();
            }
            if (!strValue.isEmpty()) {
                list.add(AdditionalInformation.builder()
                        .name(name)
                        .value(strValue)
                        .valueSchemeName(null)
                        .build());
            }
        }
    }
}