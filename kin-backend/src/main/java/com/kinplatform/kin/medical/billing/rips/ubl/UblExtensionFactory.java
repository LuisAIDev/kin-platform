package com.kinplatform.kin.medical.billing.rips.ubl;

import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UblExtensionFactory {

    public static RipsInteroperabilidadPayload fromRecord(RipsRecord record, String codigoPrestador) {
        CustomTagGeneral responsable = CustomTagGeneral.builder()
                .name("Responsable")
                .value("url www.minsalud.gov.co")
                .build();

        CustomTagGeneral actoAdministrativo = CustomTagGeneral.builder()
                .name("Tipo, identificacion del acto administrativo")
                .value("Resolucion NNNN:AAAA")
                .build();

        List<AdditionalInformation> additionalInfos = List.of(
                AdditionalInformation.builder()
                        .name("CODIGO_PRESTADOR")
                        .value(codigoPrestador)
                        .valueSchemeName("salud codigo_habilitacion")
                        .build(),
                AdditionalInformation.builder()
                        .name("MODALIDAD_PAGO")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .valueSchemeName("salud modalidad_pago")
                        .build(),
                AdditionalInformation.builder()
                        .name("COBERTURA_PLAN_BENEFICIOS")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .valueSchemeName("salud cobertura_plan")
                        .build(),
                AdditionalInformation.builder()
                        .name("NUMERO_CONTRATO")
                        .value("") // opcional
                        .build(),
                AdditionalInformation.builder()
                        .name("NUMERO_POLIZA")
                        .value("") // opcional
                        .build(),
                AdditionalInformation.builder()
                        .name("COPAGO")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .build(),
                AdditionalInformation.builder()
                        .name("CUOTA_MODERADORA")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .build(),
                AdditionalInformation.builder()
                        .name("PAGOS_COMPARTIDOS")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .build(),
                AdditionalInformation.builder()
                        .name("FECHA_INICIO_PERIODO")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .build(),
                AdditionalInformation.builder()
                        .name("FECHA_FIN_PERIODO")
                        .value(record.getRipsLineData()) // TODO: extraer del JSON
                        .build()
        );

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
}