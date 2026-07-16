package com.meingehirn.detection_svc.service;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.model.entity.PendingDetection;
import com.meingehirn.detection_svc.repository.PendingDetectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "fraudguard.redis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisSubscriberService {

    private final DetectionEngine detectionEngine;
    private final PendingDetectionRepository pendingDetectionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookService webhookService;

    public void handleMessage(String message, String channel) {
        try {
            Long txId = Long.parseLong(message);
            log.info("Received txId={} from Redis channel={}", txId, channel);

            List<String> rules = pendingDetectionRepository.findByTxId(txId)
                    .map(PendingDetection::getRules)
                    .filter(r -> r != null && !r.isBlank())
                    .map(r -> List.of(r.split(",")))
                    .orElse(Collections.emptyList());

            DetectionResult result = detectionEngine.analyze(txId, rules);
            publishResult(result);
            webhookService.deliver(result);
            markDone(txId);
        } catch (NumberFormatException e) {
            log.warn("Invalid transaction ID received from Redis: {}", message);
        }
    }

    private void publishResult(DetectionResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.convertAndSend("fraudguard.result.channel", json);
            log.info("Published result to fraudguard.result.channel: txId={} flagged={}",
                    result.getTxId(), result.isFlaggedFraud());
        } catch (Exception e) {
            log.error("Failed to publish result for txId={}", result.getTxId(), e);
        }
    }

    private void markDone(Long txId) {
        pendingDetectionRepository.findByTxId(txId).ifPresent(pd -> {
            pd.setStatus(PendingDetection.Status.DONE);
            pendingDetectionRepository.save(pd);
        });
    }
}
