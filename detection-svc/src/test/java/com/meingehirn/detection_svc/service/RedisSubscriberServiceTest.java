package com.meingehirn.detection_svc.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.repository.PendingDetectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberServiceTest {

    @Mock
    private DetectionEngine detectionEngine;

    @Mock
    private PendingDetectionRepository pendingDetectionRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private RedisSubscriberService subscriber;

    @Test
    void shouldParseMessageAndPublishResult() throws Exception {
        var result = DetectionResult.builder()
                .txId(42L)
                .tenantId("TENANT_X")
                .isFlaggedFraud(true)
                .triggeredRules(List.of("FraudCheck"))
                .evaluatedAt(Instant.now())
                .build();

        when(pendingDetectionRepository.findByTxId(42L)).thenReturn(Optional.empty());
        when(detectionEngine.analyze(eq(42L), anyList())).thenReturn(result);
        when(objectMapper.writeValueAsString(result)).thenReturn("{\"txId\":42}");

        subscriber.handleMessage("42", "fraudguard.tx.channel");

        verify(detectionEngine).analyze(eq(42L), anyList());
        verify(objectMapper).writeValueAsString(result);
        verify(redisTemplate).convertAndSend(eq("fraudguard.result.channel"), eq("{\"txId\":42}"));
    }

    @Test
    void shouldSkipInvalidMessage() {
        subscriber.handleMessage("not-a-number", "fraudguard.tx.channel");

        verify(detectionEngine, never()).analyze(anyLong(), anyList());
    }
}
