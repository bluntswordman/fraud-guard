package com.meingehirn.detection_svc.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.meingehirn.detection_svc.model.entity.Transaction;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BalanceAnomalyStrategyTest {

    private final BalanceAnomalyStrategy strategy = new BalanceAnomalyStrategy();

    @Test
    void shouldReturnBalanceAnomalyName() {
        assertThat(strategy.getName()).isEqualTo("BalanceAnomaly");
    }

    @Test
    void shouldFlagAccountDrainedToZero() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("500000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setOldBalanceOrg(new BigDecimal("500000.00"));
        tx.setNewBalanceOrig(BigDecimal.ZERO);

        assertThat(strategy.detectFraud(tx)).isTrue();
    }

    @Test
    void shouldFlagZeroBalanceAccountLargeTransaction() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("600000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setOldBalanceOrg(BigDecimal.ZERO);
        tx.setNewBalanceOrig(BigDecimal.ZERO);

        assertThat(strategy.detectFraud(tx)).isTrue();
    }

    @Test
    void shouldNotFlagNormalTransfer() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("10000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setOldBalanceOrg(new BigDecimal("50000.00"));
        tx.setNewBalanceOrig(new BigDecimal("40000.00"));

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldNotFlagPaymentType() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("PAYMENT");
        tx.setAmount(new BigDecimal("500000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setOldBalanceOrg(new BigDecimal("500000.00"));
        tx.setNewBalanceOrig(BigDecimal.ZERO);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldNotFlagZeroBalanceSmallTransaction() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("400000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setOldBalanceOrg(BigDecimal.ZERO);
        tx.setNewBalanceOrig(BigDecimal.ZERO);

        assertThat(strategy.detectFraud(tx)).isFalse();
    }

    @Test
    void shouldNotFlagPartialBalanceTransfer() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("300000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setOldBalanceOrg(new BigDecimal("500000.00"));
        tx.setNewBalanceOrig(new BigDecimal("200000.00"));

        assertThat(strategy.detectFraud(tx)).isFalse();
    }
}
