package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.TransactionInType;
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
public class TransactionInDetailResponse {

    private UUID id;
    private String reference;
    private String merchantReference;
    private TransactionInType type;
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

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private String payerAccount;
    private String payerName;
    private String payerEmail;

    private String successUrl;
    private String cancelUrl;

    private String failureReason;
    private String failureCode;

    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
