package com.kinplatform.kin.medical.billing.rips.ubl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "Collection")
public class UsuarioCollection {

    @JacksonXmlProperty(isAttribute = true, localName = "schemeName")
    private String schemeName;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "AdditionalInformation")
    private List<AdditionalInformation> additionalInformations;
}