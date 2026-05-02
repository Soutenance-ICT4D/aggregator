package com.sharepay.aggregator.modules.payment.listener;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.payment.model.UserBalance;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.shared.events.account.UserWelcomeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBalanceInitializer {

    private static final String DEFAULT_CURRENCY = "XAF";

    private final UserBalanceRepository userBalanceRepository;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserWelcome(UserWelcomeEvent event) {
        if (userBalanceRepository.findByUser_IdAndCurrency(event.getUserId(), DEFAULT_CURRENCY).isPresent()) {
            return;
        }

        User user = userRepository.getReferenceById(event.getUserId());

        UserBalance balance = UserBalance.builder()
                .user(user)
                .currency(DEFAULT_CURRENCY)
                .availableAmount(0L)
                .pendingAmount(0L)
                .build();

        userBalanceRepository.save(balance);
        log.info("Solde {} initialisé pour le marchand {} ({})", DEFAULT_CURRENCY, event.getUserId(), event.getEmail());
    }
}
