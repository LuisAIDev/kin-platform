package com.kinplatform.kin.health.common.exception;

import lombok.Data;

@Data
public class QuotaExceededException extends RuntimeException {

    private final String code;

    private final String redirectUrl;

    public QuotaExceededException(String message, String code, String redirectUrl) {
        super(message);
        this.code = code;
        this.redirectUrl = redirectUrl;
    }

    public QuotaExceededException(String message) {
        this(message, "QUOTA_EXCEEDED", null);
    }
}
