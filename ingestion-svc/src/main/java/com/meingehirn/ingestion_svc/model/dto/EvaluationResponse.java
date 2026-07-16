package com.meingehirn.ingestion_svc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {

    private String status;
    private EvaluationData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationData {
        private long truePositives;
        private long falsePositives;
        private long trueNegatives;
        private long falseNegatives;
        private long totalGroundTruth;
        private double precision;
        private double recall;
        private double f1Score;
        private double accuracy;
    }

    public static EvaluationResponse success(EvaluationData data) {
        return EvaluationResponse.builder()
                .status("success")
                .data(data)
                .build();
    }
}
