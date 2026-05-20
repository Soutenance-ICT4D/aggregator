package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import com.sharepay.aggregator.shared.constant.WithdrawalPeriod;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class WithdrawalConfigResponse {
    private WithdrawalMode mode;
    private WithdrawalAccountResponse account;
    private Long thresholdAmount;
    private WithdrawalPeriod period;
    private String currency;
    private OffsetDateTime lastTriggeredAt;
    private OffsetDateTime updatedAt;
}
