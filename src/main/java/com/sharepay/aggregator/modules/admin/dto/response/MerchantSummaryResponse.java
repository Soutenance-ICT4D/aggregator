package com.sharepay.aggregator.modules.admin.dto.response;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.KycLevel;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MerchantSummaryResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String country;
    private AccountStatus status;
    private KycLevel kycLevel;
    private boolean emailVerified;
    private OffsetDateTime createdAt;
}
