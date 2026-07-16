package com.meingehirn.detection_svc.service.strategy;

import com.meingehirn.detection_svc.model.entity.Transaction;
import com.meingehirn.detection_svc.repository.TransactionRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component("velocityCheck")
@RequiredArgsConstructor
public class VelocityStrategy implements DetectionStrategy {

    private static final int VELOCITY_WINDOW = 1;
    private static final int MAX_TRANSACTIONS_IN_WINDOW = 3;
    private static final Set<String> RISKY_TYPES = Set.of("TRANSFER", "CASH_OUT");

    private final TransactionRepository repository;

    @Override
    public String getName() {
        return "VelocityCheck";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        if (!RISKY_TYPES.contains(tx.getType())) {
            return false;
        }

        int stepStart = Math.max(0, tx.getStep() - VELOCITY_WINDOW);
        int stepEnd = tx.getStep();

        long recentCount = repository.countByNameOrigAndTypeInAndStepBetweenAndTenantId(
                tx.getNameOrig(), RISKY_TYPES, stepStart, stepEnd, tx.getTenantId());

        if (recentCount > MAX_TRANSACTIONS_IN_WINDOW) {
            log.debug("VelocityCheck: {} transactions in {} steps for account={}",
                    recentCount, VELOCITY_WINDOW + 1, tx.getNameOrig());
            return true;
        }

        return false;
    }
}
