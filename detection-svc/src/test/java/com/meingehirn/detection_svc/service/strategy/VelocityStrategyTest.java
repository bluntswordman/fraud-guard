package com.meingehirn.detection_svc.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.meingehirn.detection_svc.model.entity.Transaction;
import com.meingehirn.detection_svc.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VelocityStrategyTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private VelocityStrategy strategy;

    @Test
    void shouldReturnVelocityCheckName() {
        assertThat(strategy.getName()).isEqualTo("VelocityCheck");
    }

    @Test
    void shouldFlagWhenExceedingVelocityThreshold() {
        var tx = buildTx("TRANSFER", 1);
        var riskyTypes = Set.of("TRANSFER", "CASH_OUT");
        when(repository.countByNameOrigAndTypeInAndStepBetweenAndTenantId(
                eq("C123"), eq(riskyTypes), eq(0), eq(1), eq("PAYSIM")))
                .thenReturn(5L);

        assertThat(strategy.detectFraud(tx)).isTrue();
    }

    @Test
    void shouldNotFlagWhenUnderThreshold() {
        var tx = buildTx("TRANSFER", 1);
        var riskyTypes = Set.of("TRANSFER", "CASH_OUT");
        when(repository.countByNameOrigAndTypeInAndStepBetweenAndTenantId(
                eq("C123"), eq(riskyTypes), eq(0), eq(1), eq("PAYSIM")))
                .thenReturn(2L);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldNotFlagPaymentType() {
        var tx = buildTx("PAYMENT", 1);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldNotFlagNonRiskyType() {
        var tx = buildTx("DEBIT", 1);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldHandleStepZero() {
        var tx = buildTx("TRANSFER", 0);
        var riskyTypes = Set.of("TRANSFER", "CASH_OUT");
        when(repository.countByNameOrigAndTypeInAndStepBetweenAndTenantId(
                eq("C123"), eq(riskyTypes), eq(0), eq(0), eq("PAYSIM")))
                .thenReturn(1L);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    private Transaction buildTx(String type, int step) {
        var tx = new Transaction();
        tx.setStep(step);
        tx.setType(type);
        tx.setAmount(new BigDecimal("5000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setTenantId("PAYSIM");
        return tx;
    }
}
