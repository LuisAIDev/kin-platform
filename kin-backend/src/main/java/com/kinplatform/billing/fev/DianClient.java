package com.kinplatform.billing.fev;

public interface DianClient {

    DianResponse send(FevRipsInvoice invoice);

    DianStatusResponse queryStatus(String cufe);

    record DianResponse(boolean accepted, String cufe, String statusCode, String message, String responseXml) {
    }

    record DianStatusResponse(String cufe, String status, String message) {
    }
}
