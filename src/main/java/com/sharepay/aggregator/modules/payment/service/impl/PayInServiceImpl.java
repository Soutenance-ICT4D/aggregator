package com.sharepay.aggregator.modules.payment.service.impl;

import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.payment.dto.request.ChargeRequest;
import com.sharepay.aggregator.modules.payment.dto.request.CheckoutRequest;
import com.sharepay.aggregator.modules.payment.dto.response.ChargeResponse;
import com.sharepay.aggregator.modules.payment.dto.response.CheckoutResponse;
import com.sharepay.aggregator.modules.payment.dto.response.PayInStatusResponse;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.modules.payment.service.PayInService;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.exception.BusinessException;
import com.sharepay.aggregator.shared.gateway.PaymentGatewayRegistry;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInResponse;
import com.sharepay.aggregator.modules.payment.service.FeeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayInServiceImpl implements PayInService {

    private final TransactionInRepository transactionInRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final FeeCalculatorService feeCalculatorService;

    @Value("${app.checkout-base-url}")
    private String checkoutBaseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int CHECKOUT_EXPIRY_MINUTES = 30;

    // CHECKOUT
    @Override
    @Transactional
    public CheckoutResponse createCheckout(UUID apiKeyId, CheckoutRequest request) {
        Application application = resolveApplication(apiKeyId);

        String reference    = generateReference("PI");
        String sessionToken = generateSessionToken();

        TransactionIn tx = TransactionIn.builder()
                .reference(reference)
                .type(TransactionInType.CHECKOUT)
                .application(application)
                .sessionToken(sessionToken)
                .expiresAt(OffsetDateTime.now().plusMinutes(CHECKOUT_EXPIRY_MINUTES))
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .feeAmount(0L)
                .netAmount(request.getAmount())
                .description(request.getDescription())
                .merchantReference(request.getMerchantReference())
                .successUrl(request.getSuccessUrl())
                .cancelUrl(request.getCancelUrl())
                .status(TransactionStatus.PENDING)
                .build();

        transactionInRepository.save(tx);
        log.info("Checkout créé : {} (app: {})", reference, application.getId());

        return CheckoutResponse.builder()
                .reference(reference)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .paymentUrl(buildPaymentUrl(sessionToken))
                .build();
    }

    // CHARGE
    @Override
    @Transactional
    public ChargeResponse createCharge(UUID apiKeyId, ChargeRequest request) {
        Application application = resolveApplication(apiKeyId);

        if (request.getIdempotencyKey() != null) {
            Optional<TransactionIn> existing = transactionInRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                TransactionIn tx = existing.get();
                log.info("Charge idempotente retournée : {} (clé: {})", tx.getReference(), request.getIdempotencyKey());
                return toChargeResponse(tx);
            }
        }

        PaymentProvider provider = resolveProvider(request.getPaymentMethod());

        validateAmount(provider, request.getAmount(), request.getCurrency());

        long feeAmount = feeCalculatorService.computeFee(provider, request.getAmount());
        String reference = generateReference("PI");

        TransactionIn tx = TransactionIn.builder()
                .reference(reference)
                .type(TransactionInType.CHARGE)
                .application(application)
                .paymentProvider(provider)
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .feeAmount(feeAmount)
                .netAmount(request.getAmount() - feeAmount)
                .description(request.getDescription())
                .merchantReference(request.getMerchantReference())
                .payerAccount(request.getPayerAccount())
                .payerName(request.getPayerName())
                .payerEmail(request.getPayerEmail())
                .idempotencyKey(request.getIdempotencyKey())
                .status(TransactionStatus.PENDING)
                .build();

        transactionInRepository.save(tx);

        // Appel au provider via le gateway
        GatewayPayInResponse gwResponse = gatewayRegistry.resolve(provider.getCode())
                .initiatePayIn(GatewayPayInRequest.builder()
                        .reference(reference)
                        .providerRef(tx.getId().toString())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .payerAccount(request.getPayerAccount())
                        .payerName(request.getPayerName())
                        .description(request.getDescription())
                        .build());

        tx.setProviderTransactionId(gwResponse.getProviderTransactionId());
        transactionInRepository.save(tx);

        log.info("Charge créé : {} (app: {}, provider: {}, montant: {} {}, providerTxId: {})",
                reference, application.getId(), provider.getCode(),
                request.getAmount(), request.getCurrency(), gwResponse.getProviderTransactionId());

        return ChargeResponse.builder()
                .reference(reference)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .paymentMethod(provider.getCode())
                .payerAccount(request.getPayerAccount())
                .payerName(request.getPayerName())
                .payerEmail(request.getPayerEmail())
                .build();
    }

    // CHECK STATUS
    @Override
    @Transactional(readOnly = true)
    public PayInStatusResponse checkStatus(UUID apiKeyId, String reference) {
        Application application = resolveApplication(apiKeyId);

        TransactionIn tx = transactionInRepository
                .findByReferenceAndApplication_Id(reference, application.getId())
                .orElseThrow(() -> new BusinessException(
                        "Transaction introuvable.",
                        HttpStatus.NOT_FOUND,
                        "TRANSACTION_NOT_FOUND"
                ));

        return toStatusResponse(tx);
    }

    // Helpers privés
    private Application resolveApplication(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Clé API introuvable.", HttpStatus.UNAUTHORIZED, "API_KEY_INVALID"));
        return apiKey.getApplication();
    }

    private PaymentProvider resolveProvider(String code) {
        return paymentProviderRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        "Moyen de paiement '" + code + "' introuvable.",
                        HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND"
                ));
    }

    private void validateAmount(PaymentProvider provider, Long amount, String currency) {
        if (!provider.getCurrency().equalsIgnoreCase(currency))
            throw new BusinessException("Devise '" + currency + "' incompatible avec ce provider (attendu : " + provider.getCurrency() + ").", HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH");
        if (provider.getMinAmount() != null && amount < provider.getMinAmount())
            throw new BusinessException("Montant minimum : " + provider.getMinAmount() + " " + currency + ".", HttpStatus.BAD_REQUEST, "AMOUNT_BELOW_MINIMUM");
        if (provider.getMaxAmount() != null && amount > provider.getMaxAmount())
            throw new BusinessException("Montant maximum : " + provider.getMaxAmount() + " " + currency + ".", HttpStatus.BAD_REQUEST, "AMOUNT_ABOVE_MAXIMUM");
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

    private String buildPaymentUrl(String sessionToken) {
        return checkoutBaseUrl + sessionToken;
    }

    private ChargeResponse toChargeResponse(TransactionIn tx) {
        return ChargeResponse.builder()
                .reference(tx.getReference())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .description(tx.getDescription())
                .paymentMethod(tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null)
                .payerAccount(tx.getPayerAccount())
                .payerName(tx.getPayerName())
                .payerEmail(tx.getPayerEmail())
                .build();
    }

    private PayInStatusResponse toStatusResponse(TransactionIn tx) {
        boolean isCheckoutPending = tx.getType() == TransactionInType.CHECKOUT
                && tx.getStatus() == TransactionStatus.PENDING;

        return PayInStatusResponse.builder()
                .reference(tx.getReference())
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .description(tx.getDescription())
                .paymentMethod(tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null)
                .payerAccount(tx.getPayerAccount())
                .payerName(tx.getPayerName())
                .payerEmail(tx.getPayerEmail())
                .paymentUrl(isCheckoutPending ? buildPaymentUrl(tx.getSessionToken()) : null)
                .failureCode(tx.getFailureCode())
                .failureReason(tx.getFailureReason())
                .build();
    }
}
