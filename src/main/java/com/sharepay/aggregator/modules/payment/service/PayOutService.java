package com.sharepay.aggregator.modules.payment.service;

import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.PayOutStatusResponse;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;

import java.util.UUID;

public interface PayOutService {
    TransferResponse createTransfer(UUID apiKeyId, TransferRequest request);
    TransferResponse createTransferByUserId(UUID userId, TransferRequest request);

    /**
     * Retrait automatique (INSTANT / THRESHOLD / PERIODIC) : les frais sont déduits
     * du {@code gross} (solde à vider) plutôt qu'ajoutés par-dessus.
     * Le bénéficiaire reçoit {@code gross - fee}; le solde est débité exactement de {@code gross}.
     */
    TransferResponse createAutoWithdrawal(UUID userId, String providerCode,
                                          String beneficiaryAccount, String beneficiaryName,
                                          long gross, String currency, String description);

    PayOutStatusResponse checkStatus(UUID apiKeyId, String reference);
}
