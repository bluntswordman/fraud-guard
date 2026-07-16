package com.meingehirn.ingestion_svc.repository;

import com.meingehirn.ingestion_svc.model.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT COUNT(t) FROM Transaction t")
    long countAll();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.tenantId = :tenantId")
    long countAllByTenantId(String tenantId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")
    BigDecimal sumAllAmounts();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.tenantId = :tenantId")
    BigDecimal sumAllAmountsByTenantId(String tenantId);

    @Query("SELECT t.type, COUNT(t) FROM Transaction t GROUP BY t.type")
    List<Object[]> countByType();

    @Query("SELECT t.type, COUNT(t) FROM Transaction t WHERE t.tenantId = :tenantId GROUP BY t.type")
    List<Object[]> countByTypeAndTenantId(String tenantId);

    boolean existsByNameOrigAndStepAndAmountAndNameDest(
            String nameOrig, int step, BigDecimal amount, String nameDest);

    @Query("SELECT "
            + "COALESCE(SUM(CASE WHEN t.isFraud = true AND t.isFlaggedFraud = true THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = false AND t.isFlaggedFraud = true THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = false AND t.isFlaggedFraud = false THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = true AND t.isFlaggedFraud = false THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = true THEN 1 ELSE 0 END), 0) "
            + "FROM Transaction t")
    List<Object[]> evaluate();

    @Query("SELECT "
            + "COALESCE(SUM(CASE WHEN t.isFraud = true AND t.isFlaggedFraud = true THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = false AND t.isFlaggedFraud = true THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = false AND t.isFlaggedFraud = false THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = true AND t.isFlaggedFraud = false THEN 1 ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN t.isFraud = true THEN 1 ELSE 0 END), 0) "
            + "FROM Transaction t WHERE t.tenantId = :tenantId")
    List<Object[]> evaluateByTenantId(String tenantId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.isFraud = true")
    long countFraud();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.isFlaggedFraud = true")
    long countFlaggedFraud();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.isFlaggedFraud = true AND t.tenantId = :tenantId")
    long countFlaggedFraudByTenantId(String tenantId);

    @Query("SELECT t.type, COUNT(t) FROM Transaction t WHERE t.isFlaggedFraud = true GROUP BY t.type")
    List<Object[]> countFlaggedFraudByType();

    @Query("SELECT t.type, COUNT(t) FROM Transaction t WHERE t.isFlaggedFraud = true AND t.tenantId = :tenantId GROUP BY t.type")
    List<Object[]> countFlaggedFraudByTypeAndTenantId(String tenantId);
}
