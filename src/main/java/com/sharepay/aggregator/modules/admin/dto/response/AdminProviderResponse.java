package com.sharepay.aggregator.modules.admin.dto.response;

import com.sharepay.aggregator.shared.constant.PaymentProviderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminProviderResponse {
    private UUID id;
    private String code;
    private String name;
    private PaymentProviderType type;
    private String country;
    private String currency;
    private boolean active;
    private BigDecimal feePercentage;
    private Long feeFixed;
    private Long minAmount;
    private Long maxAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
