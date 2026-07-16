package com.meingehirn.ingestion_svc.model.dto;

import com.meingehirn.ingestion_svc.model.entity.Transaction;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionDetailResponse {
    private String status;
    private TransactionDetail data;

    @Getter
    @Builder
    public static class TransactionDetail {
        private Long id;
        private int step;
        private String type;
        private BigDecimal amount;
        private String nameOrig;
        private String nameDest;
        private BigDecimal oldBalanceOrg;
        private BigDecimal newBalanceOrig;
        private String tenantId;
        private boolean isFraud;
        private boolean isFlaggedFraud;
    }

    public static TransactionDetailResponse success(Transaction tx) {
        return TransactionDetailResponse.builder()
                .status("success")
                .data(TransactionDetail.builder()
                        .id(tx.getId())
                        .step(tx.getStep())
                        .type(tx.getType())
                        .amount(tx.getAmount())
                        .nameOrig(tx.getNameOrig())
                        .nameDest(tx.getNameDest())
                        .oldBalanceOrg(tx.getOldBalanceOrg())
                        .newBalanceOrig(tx.getNewBalanceOrig())
                        .tenantId(tx.getTenantId())
                        .isFraud(tx.getIsFraud())
                        .isFlaggedFraud(tx.getIsFlaggedFraud())
                        .build())
                .build();
    }

    public static TransactionDetailResponse notFound(Long id) {
        return TransactionDetailResponse.builder()
                .status("error")
                .data(null)
                .build();
    }
}
