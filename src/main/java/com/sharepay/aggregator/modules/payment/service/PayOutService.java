package com.sharepay.aggregator.modules.payment.service;

import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.PayOutStatusResponse;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;

import java.util.UUID;

public interface PayOutService {
    TransferResponse createTransfer(UUID apiKeyId, TransferRequest request);
    PayOutStatusResponse checkStatus(UUID apiKeyId, String reference);
}
