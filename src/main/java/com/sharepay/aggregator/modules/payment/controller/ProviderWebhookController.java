package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.payment.dto.request.ProviderWebhookPayload;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.provider.CheckoutEventPublisher;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.scheduler.TransactionPersistenceHelper;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Réception des callbacks NOKASH (champ callback_url des requêtes pay-in).
 * NOKASH envoie un POST avec : id, status, amount, phone, orderId dès qu'un
 * paiement entrant change de statut. Ce contrôleur applique la même logique
 * de transition que {@link com.sharepay.aggregator.modules.payment.scheduler.TransactionStatusScheduler},
 * qui reste actif comme filet de sécurité en cas de callback manqué.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/provider-webhook")
@Tag(name = "Webhook Provider", description = "Callbacks entrants des opérateurs de paiement (NOKASH)")
public class ProviderWebhookController {

    private final TransactionInRepository      transactionInRepository;
    private final TransactionPersistenceHelper persistenceHelper;
    private final CheckoutEventPublisher       checkoutEventPublisher;
    private final WebhookService                webhookService;

    @PostMapping("/{providerCode}")
    @Operation(summary = "Callback NOKASH (changement de statut d'un pay-in)")
    public ApiResponse<Void> handleCallback(
            @PathVariable String providerCode,
            @RequestBody ProviderWebhookPayload payload
    ) {
        log.info("[ProviderWebhook] {} - callback reçu : id={}, orderId={}, status={}",
                providerCode, payload.getId(), payload.getOrderId(), payload.getStatus());

        TransactionIn tx = transactionInRepository.findByProviderTransactionIdWithDetails(payload.getId()).orElse(null);
        if (tx == null) {
            log.warn("[ProviderWebhook] Aucune transaction pour providerTransactionId={}", payload.getId());
            return ApiResponse.success("Transaction introuvable, callback ignoré.", null);
        }

        if (payload.getOrderId() != null && !payload.getOrderId().equals(tx.getReference())) {
            log.warn("[ProviderWebhook] orderId incohérent (reçu: {}, attendu: {}) pour providerTransactionId={}",
                    payload.getOrderId(), tx.getReference(), payload.getId());
            return ApiResponse.error("ORDER_ID_MISMATCH", "orderId incohérent avec la transaction.");
        }

        if (tx.getStatus() != TransactionStatus.PENDING) {
            log.info("[ProviderWebhook] Transaction {} déjà au statut {}, callback ignoré (idempotence).",
                    tx.getReference(), tx.getStatus());
            return ApiResponse.success("Transaction déjà traitée.", null);
        }

        switch (mapStatus(payload.getStatus())) {
            case "SUCCESS" -> {
                persistenceHelper.savePayInSuccess(tx);
                checkoutEventPublisher.publish(tx.getSessionToken(), "SUCCESS", "Paiement confirmé avec succès.");
                webhookService.dispatchEvent(tx.getApplication(), "payment.success", payInData(tx));
                log.info("[ProviderWebhook] Pay-in {} → SUCCESS (callback)", tx.getReference());
            }
            case "FAILED" -> {
                persistenceHelper.savePayInFailed(tx, "PROVIDER_FAILURE", "Paiement échoué (callback provider).");
                checkoutEventPublisher.publish(tx.getSessionToken(), "FAILED", "Paiement échoué.");
                webhookService.dispatchEvent(tx.getApplication(), "payment.failed", payInData(tx));
                log.info("[ProviderWebhook] Pay-in {} → FAILED (callback)", tx.getReference());
            }
            case "CANCELLED" -> {
                persistenceHelper.savePayInCancelled(tx, "Paiement annulé (callback provider).");
                checkoutEventPublisher.publish(tx.getSessionToken(), "CANCELLED", "Paiement annulé.");
                webhookService.dispatchEvent(tx.getApplication(), "payment.cancelled", payInData(tx));
                log.info("[ProviderWebhook] Pay-in {} → CANCELLED (callback)", tx.getReference());
            }
            default -> log.debug("[ProviderWebhook] Pay-in {} toujours PENDING (callback)", tx.getReference());
        }

        return ApiResponse.success("OK", null);
    }

    private String mapStatus(String rawStatus) {
        if (rawStatus == null) return "PENDING";
        return switch (rawStatus.toUpperCase()) {
            case "SUCCESS" -> "SUCCESS";
            case "FAILED", "TIMEOUT" -> "FAILED";
            case "CANCELED", "CANCELLED" -> "CANCELLED";
            default -> "PENDING";
        };
    }

    private Map<String, Object> payInData(TransactionIn tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reference",         tx.getReference());
        m.put("type",              tx.getType().name());
        m.put("status",            tx.getStatus().name());
        m.put("amount",            tx.getAmount());
        m.put("currency",          tx.getCurrency());
        m.put("feeAmount",         tx.getFeeAmount());
        m.put("netAmount",         tx.getNetAmount());
        m.put("paymentMethod",     tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null);
        m.put("payerAccount",      tx.getPayerAccount());
        m.put("payerName",         tx.getPayerName());
        m.put("payerEmail",        tx.getPayerEmail());
        m.put("merchantReference", tx.getMerchantReference());
        m.put("description",       tx.getDescription());
        m.put("createdAt",         tx.getCreatedAt());
        m.put("updatedAt",         tx.getUpdatedAt());
        m.put("failureCode",       tx.getFailureCode());
        m.put("failureReason",     tx.getFailureReason());
        return m;
    }
}
