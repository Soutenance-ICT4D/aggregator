package com.sharepay.aggregator.modules.admin.service;

import com.sharepay.aggregator.modules.admin.dto.response.AdminTransactionResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;

import java.util.UUID;

public interface AdminTransactionService {
    PaginationResponse<AdminTransactionResponse> listTransactions(int page, int size);
    PaginationResponse<AdminTransactionResponse> listTransactionsByMerchant(UUID merchantId, int page, int size);
    AdminTransactionResponse getByReference(String reference);
}
