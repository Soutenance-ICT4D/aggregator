package com.sharepay.aggregator.modules.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class WithdrawalAccountResponse {
    private UUID id;
    private String providerCode;
    private String providerName;
    private String providerType;
    private String accountNumber;
    private String accountName;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private OffsetDateTime createdAt;
}
