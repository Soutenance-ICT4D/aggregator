package com.sharepay.aggregator.modules.account.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDashboardResponse {

    private Long availableBalance;
    private Long pendingBalance;
    private String currency;

    private Long dailyVolume;
    private int todayTransactionCount;

    private List<TransactionSummaryResponse> todayTransactions;
    private List<TransactionSummaryResponse> lastFiveTransactions;
}
