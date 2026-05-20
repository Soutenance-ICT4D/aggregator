package com.sharepay.aggregator.modules.payment.scheduler;

import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.events.payment.PayInSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Encapsule les opérations de persistance du scheduler dans des transactions
 * courtes et isolées, séparées des appels réseau vers les providers.
 */
@Component
@RequiredArgsConstructor
public class TransactionPersistenceHelper {

    private final TransactionInRepository  transactionInRepository;
    private final TransactionOutRepository transactionOutRepository;
    private final UserBalanceRepository    userBalanceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TransactionIn> loadPendingPayIns() {
        return transactionInRepository.findPendingWithDetails(OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<TransactionOut> loadPendingPayOuts() {
        return transactionOutRepository.findPendingWithDetails();
    }

    @Transactional
    public void savePayInSuccess(TransactionIn txRef) {
        // Rechargement depuis la DB : garantit que feeAmount/netAmount sont à jour
        // (le checkout les fixe dans confirmCheckout, après la création initiale à 0)
        TransactionIn tx = transactionInRepository.findByIdWithDetails(txRef.getId())
                .orElseThrow(() -> new IllegalStateException("Transaction introuvable : " + txRef.getId()));
        tx.setStatus(TransactionStatus.SUCCESS);
        transactionInRepository.save(tx);
        UUID userId = tx.getApplication().getUser().getId();
        userBalanceRepository.upsertCreditAvailableAmount(userId, tx.getCurrency(), tx.getNetAmount());
        eventPublisher.publishEvent(new PayInSuccessEvent(this, userId, tx.getNetAmount(), tx.getCurrency()));
    }

    @Transactional
    public void savePayInFailed(TransactionIn tx, String code, String reason) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureCode(code);
        tx.setFailureReason(reason);
        transactionInRepository.save(tx);
    }

    @Transactional
    public void savePayInCancelled(TransactionIn tx, String reason) {
        tx.setStatus(TransactionStatus.CANCELLED);
        tx.setFailureCode("CANCELLED");
        tx.setFailureReason(reason);
        transactionInRepository.save(tx);
    }

    @Transactional
    public void savePayOutSuccess(TransactionOut tx) {
        tx.setStatus(TransactionStatus.SUCCESS);
        transactionOutRepository.save(tx);
        UUID userId = tx.getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.decrementPendingAmount(userId, tx.getCurrency(), totalDebit);
    }

    @Transactional
    public void savePayOutFailed(TransactionOut tx, String code, String reason) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureCode(code);
        tx.setFailureReason(reason);
        transactionOutRepository.save(tx);
        UUID userId = tx.getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.rollbackPendingToAvailable(userId, tx.getCurrency(), totalDebit);
    }

    @Transactional(readOnly = true)
    public List<TransactionIn> loadExpiredPendingPayIns() {
        OffsetDateTime now    = OffsetDateTime.now();
        OffsetDateTime maxAge = now.minusHours(2);
        return transactionInRepository.findExpiredPending(now, maxAge);
    }

    @Transactional
    public void savePayInExpired(TransactionIn tx) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureCode("EXPIRED");
        tx.setFailureReason("Session expirée.");
        transactionInRepository.save(tx);
    }

    @Transactional
    public void savePayOutCancelled(TransactionOut tx, String reason) {
        tx.setStatus(TransactionStatus.CANCELLED);
        tx.setFailureCode("CANCELLED");
        tx.setFailureReason(reason);
        transactionOutRepository.save(tx);
        UUID userId = tx.getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.rollbackPendingToAvailable(userId, tx.getCurrency(), totalDebit);
    }

    @Transactional(readOnly = true)
    public List<TransactionOut> loadStuckPendingPayOuts() {
        return transactionOutRepository.findStuckPendingWithDetails(OffsetDateTime.now().minusMinutes(15));
    }

    @Transactional
    public void savePayOutStuck(TransactionOut tx) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureCode("GATEWAY_NO_RESPONSE");
        tx.setFailureReason("Aucune confirmation du provider après 15 minutes.");
        transactionOutRepository.save(tx);
        UUID userId = tx.getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.rollbackPendingToAvailable(userId, tx.getCurrency(), totalDebit);
    }

    @Transactional(readOnly = true)
    public List<TransactionIn> loadStuckPendingPayIns() {
        OffsetDateTime now    = OffsetDateTime.now();
        OffsetDateTime maxAge = now.minusMinutes(15);
        return transactionInRepository.findStuckPendingWithDetails(maxAge, now);
    }

    @Transactional
    public void savePayInStuck(TransactionIn tx) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureCode("GATEWAY_NO_RESPONSE");
        tx.setFailureReason("Aucune réponse du provider après 15 minutes.");
        transactionInRepository.save(tx);
    }
}
