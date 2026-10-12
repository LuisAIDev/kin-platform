package com.kinplatform.kin.medical.billing.rips.ubl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "AdditionalInformation")
public class AdditionalInformation {

    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Value")
    private Value value;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {

        @JacksonXmlProperty(isAttribute = true, localName = "schemeName")
        private String schemeName;

        @JacksonXmlText
        private String content;
    }
}