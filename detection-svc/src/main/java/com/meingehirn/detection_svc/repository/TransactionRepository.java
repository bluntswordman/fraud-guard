package com.meingehirn.detection_svc.repository;

import com.meingehirn.detection_svc.model.entity.Transaction;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Modifying
    @Query("UPDATE Transaction t SET t.isFlaggedFraud = true WHERE t.id = :id AND t.isFlaggedFraud = false")
    int flagAsFraudIfNotAlready(@Param("id") Long id);

    long countByNameOrigAndTypeInAndStepBetweenAndTenantId(
            String nameOrig, Set<String> types, int stepStart, int stepEnd, String tenantId);

    boolean existsByNameOrigAndTypeAndStepLessThanAndTenantId(
            String nameOrig, String type, int step, String tenantId);
}
