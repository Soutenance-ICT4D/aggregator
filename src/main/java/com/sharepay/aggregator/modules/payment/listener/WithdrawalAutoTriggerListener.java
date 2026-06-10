package com.sharepay.aggregator.modules.payment.listener;

import com.sharepay.aggregator.modules.account.repository.WithdrawalConfigRepository;
import com.sharepay.aggregator.modules.payment.scheduler.WithdrawalCircuitBreaker;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import com.sharepay.aggregator.shared.events.payment.PayInSuccessEvent;
import com.sharepay.aggregator.shared.exception.BusinessException;
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
public class WithdrawalAutoTriggerListener {

    private final WithdrawalConfigRepository configRepository;
    private final PayOutService              payOutService;
    private final WithdrawalCircuitBreaker   circuitBreaker;

    /**
     * Déclenche un retrait automatique après confirmation d'un pay-in (mode INSTANT).
     * S'exécute dans une nouvelle transaction, après commit du pay-in.
     */
    // Pas de @Transactional ici : createAutoWithdrawal gère sa propre transaction.
    // Avec REQUIRES_NEW sur le listener, un échec gateway marquerait la transaction
    // partagée rollback-only et annulerait le débit de balance + la création du payout.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPayInSuccess(PayInSuccessEvent event) {
        var config = configRepository.findByUserId(event.getUserId()).orElse(null);

        if (config == null
                || config.getMode() != WithdrawalMode.INSTANT
                || config.getAccount() == null
                || config.getAccount().isDeleted()) {
            return;
        }

        var account = config.getAccount();

        try {
            var result = payOutService.createAutoWithdrawal(
                    event.getUserId(),
                    account.getProviderCode(),
                    account.getAccountNumber(),
                    account.getAccountName(),
                    event.getNetAmount(),
                    event.getCurrency(),
                    "Retrait automatique instantané"
            );
            circuitBreaker.recordSuccess(config.getId(), null);
            log.info("[AutoWithdrawal] INSTANT déclenché - user: {}, ref: {}, gross: {} {}",
                    event.getUserId(), result.getReference(), event.getNetAmount(), event.getCurrency());
        } catch (BusinessException e) {
            if ("AMOUNT_BELOW_MINIMUM".equals(e.getCode()) || "AMOUNT_TOO_SMALL".equals(e.getCode())) {
                log.warn("[AutoWithdrawal] INSTANT - montant net après frais trop faible ({} {}), ignoré",
                        event.getNetAmount(), event.getCurrency());
                return;
            }
            circuitBreaker.recordFailure(config.getId(), e.getMessage());
            log.error("[AutoWithdrawal] INSTANT échoué - user: {} - [{}] {}",
                    event.getUserId(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            circuitBreaker.recordFailure(config.getId(), e.getMessage());
            log.error("[AutoWithdrawal] INSTANT erreur inattendue - user: {} - {}",
                    event.getUserId(), e.getMessage());
        }
    }
}
