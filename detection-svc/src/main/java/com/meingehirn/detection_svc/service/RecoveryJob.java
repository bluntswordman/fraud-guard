package com.meingehirn.detection_svc.service;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.model.entity.PendingDetection;
import com.meingehirn.detection_svc.repository.PendingDetectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "fraudguard.redis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RecoveryJob {

    private static final int RECOVERY_DELAY_SECONDS = 30;

    private final PendingDetectionRepository pendingDetectionRepository;
    private final DetectionEngine detectionEngine;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookService webhookService;

    @Scheduled(fixedRate = 30_000)
    public void recoverPending() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(RECOVERY_DELAY_SECONDS);
        List<PendingDetection> pending = pendingDetectionRepository
                .findByStatusAndCreatedAtBefore(PendingDetection.Status.PENDING, cutoff);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Recovery: found {} pending detections older than {}s", pending.size(), RECOVERY_DELAY_SECONDS);
        for (PendingDetection pd : pending) {
            try {
                List<String> rules = pd.getRules() != null && !pd.getRules().isBlank()
                        ? List.of(pd.getRules().split(","))
                        : Collections.emptyList();

                DetectionResult result = detectionEngine.analyze(pd.getTxId(), rules);
                publishResult(result);
                webhookService.deliver(result);
                pd.setStatus(PendingDetection.Status.DONE);
                pendingDetectionRepository.save(pd);
                log.info("Recovery: processed txId={}", pd.getTxId());
            } catch (Exception e) {
                log.error("Recovery: failed to process txId={}", pd.getTxId(), e);
            }
        }
    }

    private void publishResult(DetectionResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.convertAndSend("fraudguard.result.channel", json);
            log.info("Recovery: published result to fraudguard.result.channel: txId={} flagged={}",
                    result.getTxId(), result.isFlaggedFraud());
        } catch (Exception e) {
            log.error("Recovery: failed to publish result for txId={}", result.getTxId(), e);
        }
    }
}
