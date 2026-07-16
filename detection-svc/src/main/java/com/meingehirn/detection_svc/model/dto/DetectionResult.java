package com.meingehirn.detection_svc.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {

    private Long txId;
    private String tenantId;
    private boolean isFlaggedFraud;
    private List<String> triggeredRules;
    private Instant evaluatedAt;
}
