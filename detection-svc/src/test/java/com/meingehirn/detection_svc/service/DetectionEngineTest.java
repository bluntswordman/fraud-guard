package com.meingehirn.detection_svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meingehirn.detection_svc.model.dto.DetectionResult;
import com.meingehirn.detection_svc.model.entity.Transaction;
import com.meingehirn.detection_svc.repository.TransactionRepository;
import com.meingehirn.detection_svc.service.strategy.DetectionStrategy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectionEngineTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private DetectionStrategy noopStrategy;

    @Mock
    private DetectionStrategy fraudStrategy;

    @InjectMocks
    private DetectionEngine engine;

    @Test
    void shouldReturnEmptyResultWhenTransactionNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        DetectionResult result = engine.analyze(42L);

        verify(repository, never()).flagAsFraudIfNotAlready(anyLong());
        assertEquals(42L, result.getTxId());
        assertFalse(result.isFlaggedFraud());
        assertTrue(result.getTriggeredRules().isEmpty());
    }

    @Test
    void shouldReturnAlreadyFlaggedResultWhenTransactionAlreadyFlagged() {
        var tx = new Transaction();
        tx.setId(42L);
        tx.setIsFlaggedFraud(true);
        tx.setTenantId("TEST_TENANT");

        when(repository.findById(42L)).thenReturn(Optional.of(tx));

        DetectionResult result = engine.analyze(42L);

        verify(repository, never()).flagAsFraudIfNotAlready(anyLong());
        assertEquals(42L, result.getTxId());
        assertTrue(result.isFlaggedFraud());
        assertEquals("TEST_TENANT", result.getTenantId());
    }

    @Test
    void shouldRunAllStrategiesAndFlagWhenFraudDetected() {
        var tx = new Transaction();
        tx.setId(42L);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("100.00"));
        tx.setIsFlaggedFraud(false);
        tx.setTenantId("TENANT_A");

        when(repository.findById(42L)).thenReturn(Optional.of(tx));
        when(noopStrategy.getName()).thenReturn("NoopCheck");
        when(noopStrategy.detectFraud(tx)).thenReturn(false);
        when(fraudStrategy.getName()).thenReturn("FraudCheck");
        when(fraudStrategy.detectFraud(tx)).thenReturn(true);
        when(repository.flagAsFraudIfNotAlready(42L)).thenReturn(1);

        var engineWithStrats = new DetectionEngine(
                List.of(noopStrategy, fraudStrategy), repository);
        DetectionResult result = engineWithStrats.analyze(42L);

        verify(noopStrategy).detectFraud(tx);
        verify(fraudStrategy).detectFraud(tx);
        verify(repository).flagAsFraudIfNotAlready(42L);
        assertTrue(result.isFlaggedFraud());
        assertEquals("TENANT_A", result.getTenantId());
        assertEquals(List.of("FraudCheck"), result.getTriggeredRules());
    }

    @Test
    void shouldNotFlagWhenNoStrategyDetectsFraud() {
        var tx = new Transaction();
        tx.setId(42L);
        tx.setIsFlaggedFraud(false);
        tx.setTenantId("TENANT_B");

        when(repository.findById(42L)).thenReturn(Optional.of(tx));
        when(noopStrategy.getName()).thenReturn("NoopCheck");
        when(noopStrategy.detectFraud(tx)).thenReturn(false);
        when(fraudStrategy.getName()).thenReturn("AnotherCheck");
        when(fraudStrategy.detectFraud(tx)).thenReturn(false);

        var engineWithStrats = new DetectionEngine(
                List.of(noopStrategy, fraudStrategy), repository);
        DetectionResult result = engineWithStrats.analyze(42L);

        verify(repository, never()).flagAsFraudIfNotAlready(anyLong());
        assertFalse(result.isFlaggedFraud());
        assertEquals("TENANT_B", result.getTenantId());
        assertTrue(result.getTriggeredRules().isEmpty());
    }
}
