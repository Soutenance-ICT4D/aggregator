package com.sharepay.aggregator.modules.admin.service.impl;

import com.sharepay.aggregator.modules.admin.dto.response.AdminTransactionResponse;
import com.sharepay.aggregator.modules.admin.service.AdminTransactionService;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTransactionServiceImpl implements AdminTransactionService {

    private final TransactionInRepository transactionInRepository;

    @Override
    public PaginationResponse<AdminTransactionResponse> listTransactions(int page, int size) {
        Page<TransactionIn> result = transactionInRepository.findAllForAdmin(PageRequest.of(page, size));
        return toPaginationResponse(result);
    }

    @Override
    public PaginationResponse<AdminTransactionResponse> listTransactionsByMerchant(UUID merchantId, int page, int size) {
        Page<TransactionIn> result = transactionInRepository.findAllByMerchantForAdmin(merchantId, PageRequest.of(page, size));
        return toPaginationResponse(result);
    }

    @Override
    public AdminTransactionResponse getByReference(String reference) {
        TransactionIn tx = transactionInRepository.findByReferenceWithDetails(reference)
                .orElseThrow(() -> new BusinessException("Transaction introuvable.", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND"));
        return toResponse(tx);
    }

    private AdminTransactionResponse toResponse(TransactionIn tx) {
        return AdminTransactionResponse.builder()
                .id(tx.getId())
                .reference(tx.getReference())
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .feeAmount(tx.getFeeAmount())
                .netAmount(tx.getNetAmount())
                .currency(tx.getCurrency())
                .merchantName(tx.getApplication().getUser().getFullName())
                .merchantEmail(tx.getApplication().getUser().getEmail())
                .providerCode(tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null)
                .payerAccount(tx.getPayerAccount())
                .failureCode(tx.getFailureCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private PaginationResponse<AdminTransactionResponse> toPaginationResponse(Page<TransactionIn> page) {
        return PaginationResponse.<AdminTransactionResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
