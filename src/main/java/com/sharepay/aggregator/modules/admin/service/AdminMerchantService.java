package com.sharepay.aggregator.modules.admin.service;

import com.sharepay.aggregator.modules.admin.dto.request.UpdateMerchantKycRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.MerchantSummaryResponse;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.dto.PaginationResponse;

import java.util.UUID;

public interface AdminMerchantService {
    PaginationResponse<MerchantSummaryResponse> listMerchants(int page, int size, AccountStatus status);
    MerchantSummaryResponse getMerchant(UUID merchantId);
    MerchantSummaryResponse updateStatus(UUID merchantId, UpdateStatusRequest request);
    MerchantSummaryResponse updateKyc(UUID merchantId, UpdateMerchantKycRequest request);
}
