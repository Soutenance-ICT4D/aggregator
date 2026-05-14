package com.sharepay.aggregator.modules.payment.scheduler;

import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.modules.payment.provider.CheckoutEventPublisher;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.gateway.PaymentGatewayRegistry;
import com.sharepay.aggregator.shared.gateway.dto.GatewayStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionStatusScheduler {

    private final TransactionPersistenceHelper persistenceHelper;
    private final PaymentGatewayRegistry        gatewayRegistry;
    private final CheckoutEventPublisher         checkoutEventPublisher;
    private final WebhookService                 webhookService;

    /**
     * Pas de @Transactional ici : chaque appel au helper ouvre sa propre
     * transaction courte, séparée de l'appel réseau vers le provider.
     */
    @Scheduled(fixedDelay = 60_000)
    public void expireStaleCheckouts() {
        List<TransactionIn> expired = persistenceHelper.loadExpiredPendingPayIns();
        if (expired.isEmpty()) return;

        log.info("[Scheduler] {} paiement(s) expiré(s) à clôturer", expired.size());
        expired.forEach(tx -> {
            try {
                persistenceHelper.savePayInExpired(tx);
                if (tx.getSessionToken() != null) {
                    checkoutEventPublisher.publish(tx.getSessionToken(), "FAILED", "Session expirée.");
                }
                webhookService.dispatchEvent(tx.getApplication(), "payment.expired", payInData(tx));
                log.info("[Scheduler] Paiement {} → EXPIRED (expiresAt dépassé)", tx.getReference());
            } catch (OptimisticLockingFailureException e) {
                log.warn("[Scheduler] Paiement {} déjà traité par une autre instance.", tx.getReference());
            } catch (Exception e) {
                log.error("[Scheduler] Erreur expiration {} : {}", tx.getReference(), e.getMessage());
            }
        });
    }

    @Scheduled(fixedDelay = 15_000)
    public void checkPendingTransactions() {
        List<TransactionIn>  payIns  = persistenceHelper.loadPendingPayIns();
        List<TransactionOut> payOuts = persistenceHelper.loadPendingPayOuts();

        log.info("[Scheduler] {} pay-in(s) PENDING, {} pay-out(s) PENDING", payIns.size(), payOuts.size());

        payIns.forEach(this::refreshPayIn);
        payOuts.forEach(this::refreshPayOut);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void refreshPayIn(TransactionIn tx) {
        try {
            // Appel réseau hors transaction
            GatewayStatusResponse response = gatewayRegistry
                    .resolve(tx.getPaymentProvider().getCode())
                    .checkStatus(tx.getProviderTransactionId());

            switch (response.getStatus()) {
                case "SUCCESS" -> {
                    persistenceHelper.savePayInSuccess(tx);
                    checkoutEventPublisher.publish(tx.getSessionToken(), "SUCCESS", "Paiement confirmé avec succès.");
                    webhookService.dispatchEvent(tx.getApplication(), "payment.success", payInData(tx));
                    log.info("[Scheduler] Pay-in {} → SUCCESS", tx.getReference());
                }
                case "FAILED" -> {
                    persistenceHelper.savePayInFailed(tx, "PROVIDER_FAILURE", response.getMessage());
                    checkoutEventPublisher.publish(tx.getSessionToken(), "FAILED", response.getMessage());
                    webhookService.dispatchEvent(tx.getApplication(), "payment.failed", payInData(tx));
                    log.info("[Scheduler] Pay-in {} → FAILED", tx.getReference());
                }
                case "CANCELLED" -> {
                    persistenceHelper.savePayInCancelled(tx, response.getMessage());
                    checkoutEventPublisher.publish(tx.getSessionToken(), "CANCELLED", "Paiement annulé.");
                    webhookService.dispatchEvent(tx.getApplication(), "payment.cancelled", payInData(tx));
                    log.info("[Scheduler] Pay-in {} → CANCELLED", tx.getReference());
                }
                default -> log.debug("[Scheduler] Pay-in {} toujours PENDING", tx.getReference());
            }
        } catch (OptimisticLockingFailureException e) {
            log.warn("[Scheduler] Pay-in {} déjà traité par une autre instance, ignoré.", tx.getReference());
        } catch (Exception e) {
            log.error("[Scheduler] Erreur pay-in {} : {}", tx.getReference(), e.getMessage());
        }
    }

    private void refreshPayOut(TransactionOut tx) {
        try {
            // Appel réseau hors transaction
            GatewayStatusResponse response = gatewayRegistry
                    .resolve(tx.getPaymentProvider().getCode())
                    .checkStatus(tx.getProviderTransactionId());

            switch (response.getStatus()) {
                case "SUCCESS" -> {
                    persistenceHelper.savePayOutSuccess(tx);
                    webhookService.dispatchEvent(tx.getApplication(), "payout.success", payOutData(tx));
                    log.info("[Scheduler] Pay-out {} → SUCCESS", tx.getReference());
                }
                case "FAILED" -> {
                    persistenceHelper.savePayOutFailed(tx, "PROVIDER_FAILURE", response.getMessage());
                    webhookService.dispatchEvent(tx.getApplication(), "payout.failed", payOutData(tx));
                    log.info("[Scheduler] Pay-out {} → FAILED", tx.getReference());
                }
                case "CANCELLED" -> {
                    persistenceHelper.savePayOutCancelled(tx, response.getMessage());
                    webhookService.dispatchEvent(tx.getApplication(), "payout.cancelled", payOutData(tx));
                    log.info("[Scheduler] Pay-out {} → CANCELLED", tx.getReference());
                }
                default -> log.debug("[Scheduler] Pay-out {} toujours PENDING", tx.getReference());
            }
        } catch (OptimisticLockingFailureException e) {
            log.warn("[Scheduler] Pay-out {} déjà traité par une autre instance, ignoré.", tx.getReference());
        } catch (Exception e) {
            log.error("[Scheduler] Erreur pay-out {} : {}", tx.getReference(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> payInData(TransactionIn tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reference",        tx.getReference());
        m.put("type",             tx.getType().name());
        m.put("status",           tx.getStatus().name());
        m.put("amount",           tx.getAmount());
        m.put("currency",         tx.getCurrency());
        m.put("feeAmount",        tx.getFeeAmount());
        m.put("netAmount",        tx.getNetAmount());
        m.put("paymentMethod",    tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null);
        m.put("payerAccount",     tx.getPayerAccount());
        m.put("payerName",        tx.getPayerName());
        m.put("payerEmail",       tx.getPayerEmail());
        m.put("merchantReference", tx.getMerchantReference());
        m.put("description",      tx.getDescription());
        m.put("createdAt",        tx.getCreatedAt());
        m.put("updatedAt",        tx.getUpdatedAt());
        m.put("failureCode",      tx.getFailureCode());
        m.put("failureReason",    tx.getFailureReason());
        return m;
    }

    private Map<String, Object> payOutData(TransactionOut tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reference",         tx.getReference());
        m.put("status",            tx.getStatus().name());
        m.put("amount",            tx.getAmount());
        m.put("currency",          tx.getCurrency());
        m.put("feeAmount",         tx.getFeeAmount());
        m.put("netAmount",         tx.getNetAmount());
        m.put("paymentMethod",     tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null);
        m.put("beneficiaryAccount", tx.getBeneficiaryAccount());
        m.put("beneficiaryName",   tx.getBeneficiaryName());
        m.put("beneficiaryEmail",  tx.getBeneficiaryEmail());
        m.put("merchantReference", tx.getMerchantReference());
        m.put("description",       tx.getDescription());
        m.put("createdAt",         tx.getCreatedAt());
        m.put("updatedAt",         tx.getUpdatedAt());
        m.put("failureCode",       tx.getFailureCode());
        m.put("failureReason",     tx.getFailureReason());
        return m;
    }
}
