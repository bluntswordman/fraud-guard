package com.meingehirn.ingestion_svc.exception;

import lombok.Getter;

@Getter
public class InvalidCsvException extends RuntimeException {
    private final String errorCode;

    public InvalidCsvException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
