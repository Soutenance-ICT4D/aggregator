package com.sharepay.aggregator.modules.admin.dto.response;

import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminTransactionResponse {
    private UUID id;
    private String reference;
    private TransactionInType type;
    private TransactionStatus status;
    private Long amount;
    private Long feeAmount;
    private Long netAmount;
    private String currency;
    private String merchantName;
    private String merchantEmail;
    private String providerCode;
    private String payerAccount;
    private String failureCode;
    private OffsetDateTime createdAt;
}
