package com.meingehirn.detection_svc.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.meingehirn.detection_svc.model.entity.Transaction;
import com.meingehirn.detection_svc.repository.TransactionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LifecycleStrategyTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private LifecycleStrategy strategy;

    @Test
    void shouldReturnLifecycleCheckName() {
        assertThat(strategy.getName()).isEqualTo("LifecycleCheck");
    }

    @Test
    void shouldAllowTopup() {
        var tx = buildTx("TOPUP", 5);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldAllowRedeemWithPriorTopup() {
        var tx = buildTx("REDEEM", 10);
        when(repository.existsByNameOrigAndTypeAndStepLessThanAndTenantId(
                eq("C123"), eq("TOPUP"), eq(10), eq("MEMBERSHIP"))).thenReturn(true);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldFlagRedeemWithoutPriorTopup() {
        var tx = buildTx("REDEEM", 10);
        when(repository.existsByNameOrigAndTypeAndStepLessThanAndTenantId(
                eq("C123"), eq("TOPUP"), eq(10), eq("MEMBERSHIP"))).thenReturn(false);

        assertThat(strategy.detectFraud(tx)).isTrue();
    }

    @Test
    void shouldAllowEarnWithPriorRedeem() {
        var tx = buildTx("EARN", 20);
        when(repository.existsByNameOrigAndTypeAndStepLessThanAndTenantId(
                eq("C123"), eq("REDEEM"), eq(20), eq("MEMBERSHIP"))).thenReturn(true);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldFlagEarnWithoutPriorRedeem() {
        var tx = buildTx("EARN", 20);
        when(repository.existsByNameOrigAndTypeAndStepLessThanAndTenantId(
                eq("C123"), eq("REDEEM"), eq(20), eq("MEMBERSHIP"))).thenReturn(false);

        assertThat(strategy.detectFraud(tx)).isTrue();
    }

    @Test
    void shouldSkipNonMonitoredType() {
        var tx = buildTx("TRANSFER", 5);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldSkipPaymentType() {
        var tx = buildTx("PAYMENT", 5);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    private Transaction buildTx(String type, int step) {
        var tx = new Transaction();
        tx.setStep(step);
        tx.setType(type);
        tx.setAmount(new BigDecimal("5000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setTenantId("MEMBERSHIP");
        return tx;
    }
}
