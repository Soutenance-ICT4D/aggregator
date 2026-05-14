package com.sharepay.aggregator.modules.account.service;

import com.sharepay.aggregator.modules.account.dto.request.ChangePasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateProfileRequest;
import com.sharepay.aggregator.modules.account.dto.response.MerchantDashboardResponse;
import com.sharepay.aggregator.modules.account.dto.response.MerchantProfileResponse;
import com.sharepay.aggregator.modules.account.dto.response.PaymentProviderResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionSummaryResponse;
import com.sharepay.aggregator.modules.account.dto.response.UserBalanceResponse;
import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.PaginationResponse;

import java.util.List;
import java.util.UUID;

public interface MerchantService {

    MerchantProfileResponse getProfile(UUID userId);

    MerchantProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    List<UserBalanceResponse> getBalances(UUID userId);

    MerchantDashboardResponse getDashboard(UUID userId);

    TransactionChartResponse getTransactionChart(UUID userId, ChartInterval interval, ChartGroupBy groupBy);

    PaginationResponse<TransactionSummaryResponse> getTransactions(
            UUID userId,
            int page,
            int size,
            TransactionStatus status,
            TransactionInType type
    );

    List<PaymentProviderResponse> getWithdrawalProviders();

    TransferResponse initiateWithdrawal(UUID userId, TransferRequest request);
}
