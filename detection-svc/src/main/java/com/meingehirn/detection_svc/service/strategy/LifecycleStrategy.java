package com.meingehirn.detection_svc.service.strategy;

import com.meingehirn.detection_svc.model.entity.Transaction;
import com.meingehirn.detection_svc.repository.TransactionRepository;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("lifecycleCheck")
@RequiredArgsConstructor
public class LifecycleStrategy implements DetectionStrategy {

    private static final Set<String> MONITORED_TYPES = Set.of("TOPUP", "REDEEM", "EARN");

    private static final Map<String, String> REQUIRED_PRIOR = Map.of(
            "REDEEM", "TOPUP",
            "EARN", "REDEEM"
    );

    private final TransactionRepository repository;

    @Override
    public String getName() {
        return "LifecycleCheck";
    }

    @Override
    public boolean detectFraud(Transaction tx) {
        if (!MONITORED_TYPES.contains(tx.getType())) {
            return false;
        }

        String requiredPrior = REQUIRED_PRIOR.get(tx.getType());
        if (requiredPrior == null) {
            return false;
        }

        boolean hasPrior = repository.existsByNameOrigAndTypeAndStepLessThanAndTenantId(
                tx.getNameOrig(), requiredPrior,
                tx.getStep(), tx.getTenantId());

        if (!hasPrior) {
            log.warn("LifecycleCheck: {} without prior {} — account={} step={} tenant={}",
                    tx.getType(), requiredPrior, tx.getNameOrig(), tx.getStep(), tx.getTenantId());
            return true;
        }

        return false;
    }
}
