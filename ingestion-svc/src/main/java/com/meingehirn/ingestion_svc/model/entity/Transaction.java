package com.meingehirn.ingestion_svc.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int step;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "name_orig", nullable = false, length = 50)
    private String nameOrig;

    @Column(name = "name_dest", nullable = false, length = 50)
    private String nameDest;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId = "PAYSIM";

    @Column(name = "old_balance_orig", precision = 18, scale = 2)
    private BigDecimal oldBalanceOrg = BigDecimal.ZERO;

    @Column(name = "new_balance_orig", precision = 18, scale = 2)
    private BigDecimal newBalanceOrig = BigDecimal.ZERO;

    @Column(name = "is_fraud", nullable = false)
    private Boolean isFraud = false;

    @Column(name = "is_flagged_fraud", nullable = false)
    private Boolean isFlaggedFraud = false;
}
