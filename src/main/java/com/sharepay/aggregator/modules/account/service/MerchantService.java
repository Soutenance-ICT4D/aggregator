package com.sharepay.aggregator.modules.account.service;

import com.sharepay.aggregator.modules.account.dto.request.ChangePasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateProfileRequest;
import com.sharepay.aggregator.modules.account.dto.response.MerchantDashboardResponse;
import com.sharepay.aggregator.modules.account.dto.response.MerchantProfileResponse;
import com.sharepay.aggregator.modules.account.dto.response.PaymentProviderResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionInDetailResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionOutDetailResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionOutSummaryResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionStatsResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionSummaryResponse;
import com.sharepay.aggregator.modules.account.dto.response.TxChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.UserBalanceResponse;
import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.constant.TxChartCustomType;
import com.sharepay.aggregator.shared.constant.TxChartInterval;
import com.sharepay.aggregator.shared.dto.PaginationResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    // ── Transactions-in (nouveaux endpoints) ──────────────────────────────────

    TransactionStatsResponse getTransactionInStats(UUID userId);

    PaginationResponse<TransactionSummaryResponse> getTransactionsIn(
            UUID userId, int page, int size,
            TransactionStatus status, TransactionInType type,
            UUID appId, OffsetDateTime from, OffsetDateTime to
    );

    TransactionInDetailResponse getTransactionInDetail(UUID userId, UUID id);

    TxChartResponse getTransactionInChart(
            UUID userId, TxChartInterval interval, TxChartCustomType customType,
            Integer year, LocalDate fromDate, LocalDate toDate, ChartGroupBy groupBy
    );

    // ── Transactions-out (nouveaux endpoints) ─────────────────────────────────

    TransactionStatsResponse getTransactionOutStats(UUID userId);

    PaginationResponse<TransactionOutSummaryResponse> getTransactionsOut(
            UUID userId, int page, int size,
            TransactionStatus status, UUID appId,
            OffsetDateTime from, OffsetDateTime to
    );

    TransactionOutDetailResponse getTransactionOutDetail(UUID userId, UUID id);

    TxChartResponse getTransactionOutChart(
            UUID userId, TxChartInterval interval, TxChartCustomType customType,
            Integer year, LocalDate fromDate, LocalDate toDate, ChartGroupBy groupBy
    );
}
