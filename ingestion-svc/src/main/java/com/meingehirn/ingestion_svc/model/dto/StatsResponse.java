package com.meingehirn.ingestion_svc.model.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatsResponse {
    private String status;
    private StatsData data;

    @Getter
    @Builder
    public static class StatsData {
        private long totalTransactions;
        private long totalFlaggedFraud;
        private long totalClean;
        private BigDecimal totalAmount;
        private Map<String, Long> transactionsByType;
        private Map<String, Long> flaggedFraudByType;
    }

    public static StatsResponse success(long total, long flaggedFraud, BigDecimal totalAmount,
                                         Map<String, Long> byType, Map<String, Long> fraudByType) {
        return StatsResponse.builder()
                .status("success")
                .data(StatsData.builder()
                        .totalTransactions(total)
                        .totalFlaggedFraud(flaggedFraud)
                        .totalClean(total - flaggedFraud)
                        .totalAmount(totalAmount)
                        .transactionsByType(byType)
                        .flaggedFraudByType(fraudByType)
                        .build())
                .build();
    }
}
