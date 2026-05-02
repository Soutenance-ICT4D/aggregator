package com.sharepay.aggregator.modules.payment.scheduler;

import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Encapsule les opérations de persistance du scheduler dans des transactions
 * courtes et isolées, séparées des appels réseau vers les providers.
 */
@Component
@RequiredArgsConstructor
public class TransactionPersistenceHelper {

    private final TransactionInRepository transactionInRepository;
    private final TransactionOutRepository transactionOutRepository;
    private final UserBalanceRepository userBalanceRepository;

    @Transactional(readOnly = true)
    public List<TransactionIn> loadPendingPayIns() {
        return transactionInRepository.findPendingWithDetails();
    }

    @Transactional(readOnly = true)
    public List<TransactionOut> loadPendingPayOuts() {
        return transactionOutRepository.findPendingWithDetails();
    }

    @Transactional
    public void savePayInSuccess(TransactionIn tx) {
        tx.setStatus(TransactionStatus.SUCCESS);
        transactionInRepository.save(tx);
        UUID userId = tx.getApplication().getUser().getId();
        userBalanceRepository.creditAvailableAmount(userId, tx.getCurrency(), tx.getNetAmount());
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
        UUID userId = tx.getApplication().getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.decrementPendingAmount(userId, tx.getCurrency(), totalDebit);
    }

    @Transactional
    public void savePayOutFailed(TransactionOut tx, String code, String reason) {
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureCode(code);
        tx.setFailureReason(reason);
        transactionOutRepository.save(tx);
        UUID userId = tx.getApplication().getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.rollbackPendingToAvailable(userId, tx.getCurrency(), totalDebit);
    }

    @Transactional
    public void savePayOutCancelled(TransactionOut tx, String reason) {
        tx.setStatus(TransactionStatus.CANCELLED);
        tx.setFailureCode("CANCELLED");
        tx.setFailureReason(reason);
        transactionOutRepository.save(tx);
        UUID userId = tx.getApplication().getUser().getId();
        long totalDebit = tx.getAmount() + tx.getFeeAmount();
        userBalanceRepository.rollbackPendingToAvailable(userId, tx.getCurrency(), totalDebit);
    }
}
