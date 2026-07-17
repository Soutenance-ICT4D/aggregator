package com.sharepay.aggregator.modules.admin.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.admin.dto.response.AdminOverviewResponse;
import com.sharepay.aggregator.modules.admin.service.AdminStatsService;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsServiceImpl implements AdminStatsService {

    private static final int PENDING_LIMIT = 6;
    private static final int RECENT_MERCHANTS_LIMIT = 5;
    private static final int RECENT_TX_LIMIT = 8;

    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final TransactionInRepository transactionInRepository;
    private final TransactionOutRepository transactionOutRepository;

    @Override
    public AdminOverviewResponse getOverview() {
        List<AdminOverviewResponse.MerchantMini> pending = userRepository
                .findAllByRoleAndStatusOrderByCreatedAtDesc(
                        Role.MERCHANT, AccountStatus.PENDING_VERIFICATION, PageRequest.of(0, PENDING_LIMIT))
                .map(this::toMerchantMini)
                .getContent();

        List<AdminOverviewResponse.MerchantMini> recentMerchants = userRepository
                .findAllByRoleOrderByCreatedAtDesc(Role.MERCHANT, PageRequest.of(0, RECENT_MERCHANTS_LIMIT))
                .map(this::toMerchantMini)
                .getContent();

        List<AdminOverviewResponse.TxMini> recentTx = transactionInRepository
                .findAllForAdmin(PageRequest.of(0, RECENT_TX_LIMIT))
                .map(this::toTxMini)
                .getContent();

        return AdminOverviewResponse.builder()
                .payInVolume(transactionInRepository.sumAmountByStatus(TransactionStatus.SUCCESS))
                .payOutVolume(transactionOutRepository.sumAmountByStatus(TransactionStatus.SUCCESS))
                .floatAvailable(userBalanceRepository.sumAllAvailable())
                .floatPending(userBalanceRepository.sumAllPending())
                .merchantsTotal(userRepository.countByRole(Role.MERCHANT))
                .merchantsActive(userRepository.countByRoleAndStatus(Role.MERCHANT, AccountStatus.ACTIVE))
                .merchantsPending(userRepository.countByRoleAndStatus(Role.MERCHANT, AccountStatus.PENDING_VERIFICATION))
                .txTotal(transactionInRepository.count())
                .txSuccess(transactionInRepository.countByStatus(TransactionStatus.SUCCESS))
                .pendingVerification(pending)
                .recentMerchants(recentMerchants)
                .recentTransactions(recentTx)
                .build();
    }

    private AdminOverviewResponse.MerchantMini toMerchantMini(User user) {
        return AdminOverviewResponse.MerchantMini.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus())
                .kycLevel(user.getKycLevel())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AdminOverviewResponse.TxMini toTxMini(TransactionIn tx) {
        return AdminOverviewResponse.TxMini.builder()
                .id(tx.getId())
                .reference(tx.getReference())
                .merchantName(tx.getApplication().getUser().getFullName())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
