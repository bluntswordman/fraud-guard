package com.meingehirn.ingestion_svc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.meingehirn.ingestion_svc.model.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Test
    void shouldSaveAndFindById() {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType("TRANSFER");
        tx.setAmount(new BigDecimal("181.00"));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");

        var saved = repository.save(tx);
        assertThat(saved.getId()).isNotNull();

        var found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("181.00");
    }

    @Test
    void shouldSaveAllInBatch() {
        var tx1 = createTransaction("TRANSFER", "100.00");
        var tx2 = createTransaction("CASH_OUT", "200.00");
        var tx3 = createTransaction("PAYMENT", "50.00");

        var saved = repository.saveAll(List.of(tx1, tx2, tx3));
        assertThat(saved).hasSize(3);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        var found = repository.findById(999L);
        assertThat(found).isEmpty();
    }

    private Transaction createTransaction(String type, String amount) {
        var tx = new Transaction();
        tx.setStep(1);
        tx.setType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setNameOrig("C123");
        tx.setNameDest("M456");
        return tx;
    }
}
