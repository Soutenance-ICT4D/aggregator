package com.sharepay.aggregator.modules.account.service;

import com.sharepay.aggregator.modules.account.dto.response.MerchantDashboardResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.UserBalanceResponse;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;

import java.util.List;
import java.util.UUID;

public interface MerchantService {

    List<UserBalanceResponse> getBalances(UUID userId);

    MerchantDashboardResponse getDashboard(UUID userId);

    TransactionChartResponse getTransactionChart(UUID userId, ChartInterval interval, ChartGroupBy groupBy);
}
