package com.meingehirn.ingestion_svc.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private String status;
    private String message;
    private String errorCode;

    public static ErrorResponse of(String message, String errorCode) {
        return ErrorResponse.builder()
                .status("error")
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
