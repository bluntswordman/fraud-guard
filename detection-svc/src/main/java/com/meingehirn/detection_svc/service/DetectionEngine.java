package com.meingehirn.detection_svc.service;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.model.entity.Transaction;
import com.meingehirn.detection_svc.repository.TransactionRepository;
import com.meingehirn.detection_svc.service.strategy.DetectionStrategy;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionEngine {

    private final List<DetectionStrategy> strategies;
    private final TransactionRepository repository;

    @Transactional
    public DetectionResult analyze(Long txId) {
        return analyze(txId, Collections.emptyList());
    }

    @Transactional
    public DetectionResult analyze(Long txId, List<String> rules) {
        Transaction tx = repository.findById(txId).orElse(null);
        if (tx == null) {
            log.warn("Transaction not found for id={}", txId);
            return DetectionResult.builder()
                    .txId(txId)
                    .isFlaggedFraud(false)
                    .triggeredRules(List.of())
                    .evaluatedAt(Instant.now())
                    .build();
        }

        if (Boolean.TRUE.equals(tx.getIsFlaggedFraud())) {
            log.info("Transaction {} already flagged, skipping", txId);
            return DetectionResult.builder()
                    .txId(txId)
                    .tenantId(tx.getTenantId())
                    .isFlaggedFraud(true)
                    .triggeredRules(List.of())
                    .evaluatedAt(Instant.now())
                    .build();
        }

        List<DetectionStrategy> activeStrategies = strategies;
        if (rules != null && !rules.isEmpty()) {
            Set<String> ruleSet = Set.copyOf(rules);
            activeStrategies = strategies.stream()
                    .filter(s -> ruleSet.contains(s.getName()))
                    .collect(Collectors.toList());
        }

        List<String> triggered = activeStrategies.stream()
                .filter(strategy -> {
                    boolean result = strategy.detectFraud(tx);
                    log.info("Running {} -> {}", strategy.getName(), result ? "FRAUD" : "CLEAN");
                    return result;
                })
                .map(DetectionStrategy::getName)
                .collect(Collectors.toList());

        boolean fraudDetected = !triggered.isEmpty();

        if (fraudDetected) {
            int updated = repository.flagAsFraudIfNotAlready(txId);
            if (updated > 0) {
                log.info("Transaction {} flagged as fraud", txId);
            }
        }

        log.info("event=fraud_result txId={} tenantId={} flagged={} triggered={}",
                txId, tx.getTenantId(), fraudDetected, triggered);

        return DetectionResult.builder()
                .txId(txId)
                .tenantId(tx.getTenantId())
                .isFlaggedFraud(fraudDetected)
                .triggeredRules(triggered)
                .evaluatedAt(Instant.now())
                .build();
    }
}
