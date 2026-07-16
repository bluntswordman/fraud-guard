package com.meingehirn.detection_svc.service.strategy;

import com.meingehirn.detection_svc.model.entity.Transaction;
import java.math.BigDecimal;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("balanceAnomaly")
public class BalanceAnomalyStrategy implements DetectionStrategy {

    private static final Set<String> RISKY_TYPES = Set.of("TRANSFER", "CASH_OUT");
    private static final BigDecimal ZERO_BALANCE_HIGH_AMOUNT = new BigDecimal("500000.00");

    @Override
    public String getName() {
        return "BalanceAnomaly";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        if (!RISKY_TYPES.contains(tx.getType())) {
            return false;
        }

        if (tx.getAmount().compareTo(BigDecimal.ZERO) > 0
                && tx.getOldBalanceOrg() != null
                && tx.getOldBalanceOrg().compareTo(tx.getAmount()) >= 0
                && tx.getNewBalanceOrig() != null
                && tx.getNewBalanceOrig().compareTo(BigDecimal.ZERO) == 0) {
            log.debug("BalanceAnomaly: account drained to zero. account={} amount={}",
                    tx.getNameOrig(), tx.getAmount());
            return true;
        }

        if (tx.getOldBalanceOrg() != null
                && tx.getOldBalanceOrg().compareTo(BigDecimal.ZERO) == 0
                && tx.getAmount().compareTo(ZERO_BALANCE_HIGH_AMOUNT) > 0) {
            log.debug("BalanceAnomaly: large transaction from zero-balance account. account={} amount={}",
                    tx.getNameOrig(), tx.getAmount());
            return true;
        }

        return false;
    }
}
