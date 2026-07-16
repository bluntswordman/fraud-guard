package com.meingehirn.ingestion_svc.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    private int step;

    @NotNull
    @NotBlank
    private String type;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String nameOrig;

    @NotBlank
    private String nameDest;

    private BigDecimal oldBalanceOrg;

    private BigDecimal newBalanceOrig;

    private Boolean isFraud;

    @NotBlank
    private String tenantId;

    private List<String> rules;
}
