package com.kinplatform.kin.medical.billing.fev;

public interface XmlSigner {

    Signature sign(String xml);

    record Signature(String signedXml, String cufe, String qrCode) {
    }
}

