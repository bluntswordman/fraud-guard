package com.meingehirn.detection_svc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.meingehirn.detection_svc.model.entity.Transaction;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void shouldFlagAsFraudWhenNotAlreadyFlagged() {
        var tx = createAndSaveTransaction(false);

        int updated = repository.flagAsFraudIfNotAlready(tx.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        var found = repository.findById(tx.getId()).orElseThrow();
        assertThat(found.getIsFlaggedFraud()).isTrue();
    }

    @Test
    @Transactional
    void shouldNotUpdateWhenAlreadyFlagged() {
        var tx = createAndSaveTransaction(true);

        int updated = repository.flagAsFraudIfNotAlready(tx.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(updated).isZero();
        var found = repository.findById(tx.getId()).orElseThrow();
        assertThat(found.getIsFlaggedFraud()).isTrue();
    }

    @Test
    @Transactional
    void shouldUpdateOnlyTargetTransaction() {
        var tx1 = createAndSaveTransaction(false);
        var tx2 = createAndSaveTransaction(false);

        repository.flagAsFraudIfNotAlready(tx1.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(tx1.getId()).orElseThrow().getIsFlaggedFraud()).isTrue();
        assertThat(repository.findById(tx2.getId()).orElseThrow().getIsFlaggedFraud()).isFalse();
    }

    private Transaction createAndSaveTransaction(boolean flagged) {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("1000.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        tx.setIsFlaggedFraud(flagged);
        return repository.save(tx);
    }
}
