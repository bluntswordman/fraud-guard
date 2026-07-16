package com.meingehirn.ingestion_svc.model.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadResponse {
    private String status;
    private String message;
    private UploadData data;

    @Getter
    @Builder
    public static class UploadData {
        private String fileName;
        private int totalRows;
        private int processedRows;
        private int skippedRows;
        private int publishedEvents;
    }

    public static UploadResponse success(String fileName, int totalRows, int processedRows, int skippedRows, int publishedEvents) {
        return UploadResponse.builder()
                .status("success")
                .message("CSV processed successfully")
                .data(UploadData.builder()
                        .fileName(fileName)
                        .totalRows(totalRows)
                        .processedRows(processedRows)
                        .skippedRows(skippedRows)
                        .publishedEvents(publishedEvents)
                        .build())
                .build();
    }
}
