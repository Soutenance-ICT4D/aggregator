package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionOutDetailResponse {

    private UUID id;
    private String reference;
    private String merchantReference;
    private Long amount;
    private Long feeAmount;
    private Long netAmount;
    private String currency;
    private TransactionStatus status;
    private String description;

    private String provider;
    private String providerTransactionId;

    private UUID appId;
    private String appName;

    private String beneficiaryName;
    private String beneficiaryEmail;
    private String beneficiaryAccount;

    private String failureReason;
    private String failureCode;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
