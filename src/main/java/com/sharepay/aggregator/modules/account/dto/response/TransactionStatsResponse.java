package com.sharepay.aggregator.modules.account.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatsResponse {
    private long total;
    private long successCount;
    private long pendingCount;
    private long failedCount;
    private long cancelledCount;
}
