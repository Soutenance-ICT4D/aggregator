package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.modules.collect.repository.FundCollectionRepository;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.provider.CheckoutEventPublisher;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import com.sharepay.aggregator.shared.gateway.PaymentGatewayRegistry;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class PaymentPublicController {

    private final TransactionInRepository   transactionInRepository;
    private final FundCollectionRepository  fundCollectionRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentGatewayRegistry    gatewayRegistry;
    private final CheckoutEventPublisher    eventPublisher;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int CHECKOUT_EXPIRY_MINUTES = 30;

    // ─────────────────────────────────────────────────────────────────────────
    // SSE — le browser s'abonne ici après soumission d'un formulaire
    // Utilisé pour les deux flows : checkout et collect
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/checkout/events/{sessionToken}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@PathVariable String sessionToken) {
        transactionInRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException("Session introuvable.", HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND"));

        return eventPublisher.register(sessionToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Confirmation paiement Checkout (lien marchand)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/checkout/confirm")
    @Transactional
    public ApiResponse<Map<String, String>> confirmCheckout(
            @RequestParam String sessionToken,
            @RequestParam String providerCode,
            @RequestParam String payerAccount,
            @RequestParam(required = false) String payerName,
            @RequestParam(required = false) String customerEmail
    ) {
        TransactionIn tx = transactionInRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException("Session introuvable.", HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND"));

        if (tx.getType() != TransactionInType.CHECKOUT) {
            throw new BusinessException("Session invalide.", HttpStatus.BAD_REQUEST, "INVALID_SESSION");
        }
        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Cette session a déjà été traitée.", HttpStatus.CONFLICT, "SESSION_ALREADY_PROCESSED");
        }
        if (tx.getExpiresAt() != null && tx.getExpiresAt().isBefore(OffsetDateTime.now())) {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureCode("EXPIRED");
            tx.setFailureReason("Session expirée.");
            transactionInRepository.save(tx);
            throw new BusinessException("Session expirée.", HttpStatus.GONE, "SESSION_EXPIRED");
        }

        PaymentProvider provider = resolveActiveProvider(providerCode, tx.getCurrency());

        long feeAmount = computeFee(provider, tx.getAmount());
        long netAmount = tx.getAmount() - feeAmount;

        tx.setPaymentProvider(provider);
        tx.setPayerAccount(payerAccount);
        tx.setPayerName(payerName);
        tx.setCustomerEmail(customerEmail);
        tx.setFeeAmount(feeAmount);
        tx.setNetAmount(netAmount);
        transactionInRepository.save(tx);

        GatewayPayInResponse gwResponse = gatewayRegistry.resolve(provider.getCode())
                .initiatePayIn(GatewayPayInRequest.builder()
                        .reference(tx.getReference())
                        .providerRef(tx.getId().toString())
                        .amount(tx.getAmount())
                        .currency(tx.getCurrency())
                        .payerAccount(payerAccount)
                        .payerName(payerName)
                        .description(tx.getDescription())
                        .build());

        tx.setProviderTransactionId(gwResponse.getProviderTransactionId());
        transactionInRepository.save(tx);

        log.info("[Checkout] Paiement initié — ref: {}, provider: {}, montant: {} {}",
                tx.getReference(), providerCode, tx.getAmount(), tx.getCurrency());

        return ApiResponse.success("Paiement initié, en attente de confirmation.",
                Map.of("reference", tx.getReference(), "sessionToken", sessionToken));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Confirmation paiement Collecte (page publique via slug)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/collect/confirm")
    @Transactional
    public ApiResponse<Map<String, String>> confirmCollect(
            @RequestParam String slug,
            @RequestParam String providerCode,
            @RequestParam String payerAccount,
            @RequestParam(required = false) String payerName,
            @RequestParam(required = false) Long amount,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerPhone
    ) {
        // ── Validation de la collecte ──────────────────────────────────────
        FundCollection collection = fundCollectionRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(
                        "Collecte introuvable.", HttpStatus.NOT_FOUND, "COLLECT_NOT_FOUND"));

        if (collection.getStatus() != FundCollectionStatus.ACTIVE) {
            throw new BusinessException(
                    "Cette collecte n'est plus active.", HttpStatus.CONFLICT, "COLLECT_NOT_ACTIVE");
        }
        if (collection.getExpiresAt() != null && collection.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(
                    "Cette collecte a expiré.", HttpStatus.GONE, "COLLECT_EXPIRED");
        }

        // ── Résolution du montant ──────────────────────────────────────────
        long finalAmount;
        if (collection.isAmountFixed()) {
            finalAmount = collection.getAmount();
        } else {
            if (amount == null || amount <= 0) {
                throw new BusinessException(
                        "Le montant est requis pour cette collecte.", HttpStatus.BAD_REQUEST, "AMOUNT_REQUIRED");
            }
            finalAmount = amount;
        }

        // ── Validation du provider ─────────────────────────────────────────
        PaymentProvider provider = resolveActiveProvider(providerCode, collection.getCurrency());

        if (provider.getMinAmount() != null && finalAmount < provider.getMinAmount()) {
            throw new BusinessException(
                    "Montant minimum : " + provider.getMinAmount() + " " + collection.getCurrency() + ".",
                    HttpStatus.BAD_REQUEST, "AMOUNT_BELOW_MINIMUM");
        }
        if (provider.getMaxAmount() != null && finalAmount > provider.getMaxAmount()) {
            throw new BusinessException(
                    "Montant maximum : " + provider.getMaxAmount() + " " + collection.getCurrency() + ".",
                    HttpStatus.BAD_REQUEST, "AMOUNT_ABOVE_MAXIMUM");
        }

        // ── Création de la transaction ─────────────────────────────────────
        long feeAmount    = computeFee(provider, finalAmount);
        long netAmount    = finalAmount - feeAmount;
        String reference  = generateReference("FC");
        String sessionToken = generateSessionToken();

        TransactionIn tx = TransactionIn.builder()
                .reference(reference)
                .type(TransactionInType.FUND_COLLECTION)
                .application(collection.getApplication())
                .fundCollection(collection)
                .paymentProvider(provider)
                .sessionToken(sessionToken)
                .expiresAt(OffsetDateTime.now().plusMinutes(CHECKOUT_EXPIRY_MINUTES))
                .currency(collection.getCurrency())
                .amount(finalAmount)
                .feeAmount(feeAmount)
                .netAmount(netAmount)
                .description(collection.getTitle())
                .payerAccount(payerAccount)
                .payerName(payerName)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .status(TransactionStatus.PENDING)
                .build();

        transactionInRepository.save(tx);

        // ── Appel au gateway ───────────────────────────────────────────────
        GatewayPayInResponse gwResponse = gatewayRegistry.resolve(provider.getCode())
                .initiatePayIn(GatewayPayInRequest.builder()
                        .reference(reference)
                        .providerRef(tx.getId().toString())
                        .amount(finalAmount)
                        .currency(collection.getCurrency())
                        .payerAccount(payerAccount)
                        .payerName(payerName)
                        .description(collection.getTitle())
                        .build());

        tx.setProviderTransactionId(gwResponse.getProviderTransactionId());
        transactionInRepository.save(tx);

        log.info("[Collect] Paiement initié — ref: {}, slug: {}, provider: {}, montant: {} {}",
                reference, slug, providerCode, finalAmount, collection.getCurrency());

        return ApiResponse.success("Paiement initié, en attente de confirmation.",
                Map.of("reference", reference, "sessionToken", sessionToken));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Statut (fallback polling si SSE non disponible)
    // Utilisé pour les deux flows : checkout et collect
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/checkout/status/{sessionToken}")
    public ApiResponse<java.util.HashMap<String, String>> status(@PathVariable String sessionToken) {
        TransactionIn tx = transactionInRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new BusinessException("Session introuvable.", HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND"));

        java.util.HashMap<String, String> data = new java.util.HashMap<>();
        data.put("status",    tx.getStatus().name());
        data.put("reference", tx.getReference());
        data.put("returnUrl", tx.getSuccessUrl() != null ? tx.getSuccessUrl() : "#");
        return ApiResponse.success(data);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private PaymentProvider resolveActiveProvider(String providerCode, String expectedCurrency) {
        PaymentProvider provider = paymentProviderRepository.findByCode(providerCode)
                .orElseThrow(() -> new BusinessException(
                        "Moyen de paiement introuvable.", HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND"));

        if (!provider.isActive()) {
            throw new BusinessException(
                    "Moyen de paiement indisponible.", HttpStatus.BAD_REQUEST, "PROVIDER_INACTIVE");
        }
        if (!provider.getCurrency().equalsIgnoreCase(expectedCurrency)) {
            throw new BusinessException(
                    "Devise incompatible avec ce moyen de paiement.", HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH");
        }
        return provider;
    }

    private long computeFee(PaymentProvider provider, long amount) {
        long fee = 0L;
        if (provider.getFeePercentage() != null)
            fee += Math.round(amount * provider.getFeePercentage().doubleValue() / 100.0);
        if (provider.getFeeFixed() != null)
            fee += provider.getFeeFixed();
        return fee;
    }

    private String generateReference(String prefix) {
        byte[] bytes = new byte[6];
        secureRandom.nextBytes(bytes);
        return prefix + "-" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return "cs_" + HexFormat.of().formatHex(bytes);
    }
}
