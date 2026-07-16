package com.meingehirn.detection_svc.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionEntityTest {

    @Test
    void shouldBuildTransactionWithAllFields() {
        var tx = Transaction.builder()
                .step(1)
                .type("TRANSFER")
                .amount(new BigDecimal("181.00"))
                .nameOrig("C1305486145")
                .nameDest("C553264065")
                .oldBalanceOrg(new BigDecimal("181.00"))
                .newBalanceOrig(BigDecimal.ZERO)
                .isFraud(false)
                .isFlaggedFraud(false)
                .build();

        assertThat(tx.getStep()).isEqualTo(1);
        assertThat(tx.getType()).isEqualTo("TRANSFER");
        assertThat(tx.getAmount()).isEqualByComparingTo("181.00");
        assertThat(tx.getNameOrig()).isEqualTo("C1305486145");
        assertThat(tx.getNameDest()).isEqualTo("C553264065");
        assertThat(tx.getOldBalanceOrg()).isEqualByComparingTo("181.00");
        assertThat(tx.getNewBalanceOrig()).isEqualByComparingTo("0.00");
        assertThat(tx.getIsFraud()).isFalse();
        assertThat(tx.getIsFlaggedFraud()).isFalse();
    }

    @Test
    void shouldUseDefaultValuesWhenNotSet() {
        var tx = new Transaction();

        assertThat(tx.getOldBalanceOrg()).isEqualByComparingTo("0.00");
        assertThat(tx.getNewBalanceOrig()).isEqualByComparingTo("0.00");
        assertThat(tx.getIsFraud()).isFalse();
        assertThat(tx.getIsFlaggedFraud()).isFalse();
    }

    @Test
    void shouldSetAndGetId() {
        var tx = new Transaction();
        tx.setId(1L);
        assertThat(tx.getId()).isEqualTo(1L);
    }

    @Test
    void shouldHandleBigDecimalPrecision() {
        var tx = Transaction.builder()
                .amount(new BigDecimal("9999999999999999.99"))
                .build();

        assertThat(tx.getAmount()).isEqualByComparingTo("9999999999999999.99");
    }
}
