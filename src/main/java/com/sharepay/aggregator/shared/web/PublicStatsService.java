package com.sharepay.aggregator.shared.web;

import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.dto.PublicStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicStatsService {

    private final UserRepository userRepository;
    private final TransactionInRepository transactionInRepository;
    private final PaymentProviderRepository paymentProviderRepository;

    @Transactional(readOnly = true)
    public PublicStatsResponse getStats() {
        long merchantCount      = userRepository.countByRoleAndStatus(Role.MERCHANT, AccountStatus.ACTIVE);
        long transactionCount   = transactionInRepository.count();
        long paymentMethodCount = paymentProviderRepository.countByIsActiveTrue();
        return new PublicStatsResponse(merchantCount, transactionCount, paymentMethodCount);
    }
}
