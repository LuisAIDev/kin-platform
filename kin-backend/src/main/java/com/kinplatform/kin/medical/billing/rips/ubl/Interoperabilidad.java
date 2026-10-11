package com.kinplatform.kin.medical.billing.rips.ubl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "Interoperabilidad")
public class Interoperabilidad {

    @JacksonXmlProperty(localName = "Group")
    private InteroperabilidadGroup group;
}