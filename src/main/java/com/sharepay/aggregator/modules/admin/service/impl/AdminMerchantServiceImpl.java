package com.sharepay.aggregator.modules.admin.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateMerchantKycRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.Merchant360Response;
import com.sharepay.aggregator.modules.admin.dto.response.MerchantSummaryResponse;
import com.sharepay.aggregator.modules.admin.service.AdminMerchantService;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.modules.payment.model.UserBalance;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMerchantServiceImpl implements AdminMerchantService {

    private final UserRepository userRepository;
    private final TransactionInRepository transactionInRepository;
    private final TransactionOutRepository transactionOutRepository;
    private final UserBalanceRepository userBalanceRepository;

    @Override
    public PaginationResponse<MerchantSummaryResponse> listMerchants(int page, int size, AccountStatus status) {
        Page<User> result = (status != null)
                ? userRepository.findAllByRoleAndStatusOrderByCreatedAtDesc(Role.MERCHANT, status, PageRequest.of(page, size))
                : userRepository.findAllByRoleOrderByCreatedAtDesc(Role.MERCHANT, PageRequest.of(page, size));
        return toPaginationResponse(result);
    }

    @Override
    public MerchantSummaryResponse getMerchant(UUID merchantId) {
        return toResponse(resolveMerchant(merchantId));
    }

    @Override
    public Merchant360Response getOverview(UUID merchantId) {
        User merchant = resolveMerchant(merchantId);

        List<Merchant360Response.BalanceInfo> balances = userBalanceRepository
                .findByUser_IdOrderByCurrencyAsc(merchantId)
                .stream()
                .map(this::toBalanceInfo)
                .toList();

        List<TransactionIn> payIns = transactionInRepository.findAllByMerchant(merchantId);
        List<TransactionOut> payOuts = transactionOutRepository.findAllByUser(merchantId);

        return Merchant360Response.builder()
                .id(merchant.getId())
                .fullName(merchant.getFullName())
                .email(merchant.getEmail())
                .phone(merchant.getPhone())
                .country(merchant.getCountry())
                .avatarUrl(merchant.getAvatarUrl())
                .status(merchant.getStatus())
                .kycLevel(merchant.getKycLevel())
                .emailVerified(merchant.isEmailVerified())
                .phoneVerified(merchant.isPhoneVerified())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .balances(balances)
                .payInStats(buildStats(payIns, TransactionIn::getStatus, TransactionIn::getAmount))
                .payOutStats(buildStats(payOuts, TransactionOut::getStatus, TransactionOut::getAmount))
                .transactionsIn(payIns.stream().map(this::toTransactionInInfo).toList())
                .payouts(payOuts.stream().map(this::toPayoutInfo).toList())
                .build();
    }

    @Override
    @Transactional
    public MerchantSummaryResponse updateStatus(UUID merchantId, UpdateStatusRequest request) {
        User merchant = resolveMerchant(merchantId);
        merchant.setStatus(request.getStatus());
        return toResponse(userRepository.save(merchant));
    }

    @Override
    @Transactional
    public MerchantSummaryResponse updateKyc(UUID merchantId, UpdateMerchantKycRequest request) {
        User merchant = resolveMerchant(merchantId);
        merchant.setKycLevel(request.getKycLevel());
        return toResponse(userRepository.save(merchant));
    }

    /** Construit les compteurs et montants par statut, avec une entrée pour chaque statut (zéro compris). */
    private <T> Merchant360Response.TransactionStats buildStats(
            List<T> items,
            Function<T, TransactionStatus> statusFn,
            Function<T, Long> amountFn
    ) {
        Map<TransactionStatus, Merchant360Response.StatusBucket> byStatus = new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus s : TransactionStatus.values()) {
            byStatus.put(s, Merchant360Response.StatusBucket.builder().count(0L).sumAmount(0L).build());
        }

        long successVolume = 0L;
        for (T item : items) {
            TransactionStatus status = statusFn.apply(item);
            Long raw = amountFn.apply(item);
            long amount = raw != null ? raw : 0L;

            Merchant360Response.StatusBucket bucket = byStatus.get(status);
            bucket.setCount(bucket.getCount() + 1);
            bucket.setSumAmount(bucket.getSumAmount() + amount);

            if (status == TransactionStatus.SUCCESS) {
                successVolume += amount;
            }
        }

        return Merchant360Response.TransactionStats.builder()
                .totalCount(items.size())
                .totalVolume(successVolume)
                .byStatus(byStatus)
                .build();
    }

    private Merchant360Response.BalanceInfo toBalanceInfo(UserBalance b) {
        return Merchant360Response.BalanceInfo.builder()
                .currency(b.getCurrency())
                .availableAmount(b.getAvailableAmount())
                .pendingAmount(b.getPendingAmount())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private Merchant360Response.TransactionInInfo toTransactionInInfo(TransactionIn tx) {
        return Merchant360Response.TransactionInInfo.builder()
                .id(tx.getId())
                .reference(tx.getReference())
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .feeAmount(tx.getFeeAmount())
                .netAmount(tx.getNetAmount())
                .currency(tx.getCurrency())
                .providerName(providerName(tx.getPaymentProvider()))
                .payerAccount(tx.getPayerAccount())
                .customerName(tx.getCustomerName())
                .failureCode(tx.getFailureCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private Merchant360Response.PayoutInfo toPayoutInfo(TransactionOut tx) {
        return Merchant360Response.PayoutInfo.builder()
                .id(tx.getId())
                .reference(tx.getReference())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .feeAmount(tx.getFeeAmount())
                .netAmount(tx.getNetAmount())
                .currency(tx.getCurrency())
                .providerName(providerName(tx.getPaymentProvider()))
                .beneficiaryName(tx.getBeneficiaryName())
                .beneficiaryAccount(tx.getBeneficiaryAccount())
                .failureCode(tx.getFailureCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private String providerName(PaymentProvider provider) {
        return provider != null ? provider.getName() : null;
    }

    private User resolveMerchant(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Marchand introuvable.", HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND"));
        if (user.getRole() != Role.MERCHANT) {
            throw new BusinessException("Cet utilisateur n'est pas un marchand.", HttpStatus.BAD_REQUEST, "NOT_MERCHANT");
        }
        return user;
    }

    private MerchantSummaryResponse toResponse(User user) {
        return MerchantSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .country(user.getCountry())
                .status(user.getStatus())
                .kycLevel(user.getKycLevel())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PaginationResponse<MerchantSummaryResponse> toPaginationResponse(Page<User> page) {
        return PaginationResponse.<MerchantSummaryResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
