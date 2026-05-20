package com.sharepay.aggregator.modules.payment.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.PayOutStatusResponse;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.exception.BusinessException;
import com.sharepay.aggregator.shared.gateway.PaymentGatewayRegistry;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayOutRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayOutResponse;
import com.sharepay.aggregator.modules.payment.service.FeeCalculatorService;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOutServiceImpl implements PayOutService {

    private final TransactionOutRepository transactionOutRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final FeeCalculatorService feeCalculatorService;
    private final WebhookService webhookService;

    private final SecureRandom secureRandom = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────────
    // TRANSFER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse createTransfer(UUID apiKeyId, TransferRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Clé API introuvable.", HttpStatus.UNAUTHORIZED, "API_KEY_INVALID"));
        Application application = apiKey.getApplication();
        return doTransfer(application.getUser(), application, request);
    }

    /** Retrait depuis le dashboard marchand — pas de webhook. */
    @Override
    @Transactional
    public TransferResponse createTransferByUserId(UUID userId, TransferRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        return doTransfer(user, null, request);
    }

    /** Retrait automatique : frais déduits du gross (solde à vider), bénéficiaire reçoit gross - fee. */
    @Override
    @Transactional
    public TransferResponse createAutoWithdrawal(UUID userId, String providerCode,
                                                  String beneficiaryAccount, String beneficiaryName,
                                                  long gross, String currency, String description) {
        PaymentProvider provider = resolveProvider(providerCode);

        long amount = feeCalculatorService.computeNetFromGross(provider, gross);
        if (amount <= 0) {
            throw new BusinessException(
                    "Montant trop faible après déduction des frais de retrait.",
                    HttpStatus.BAD_REQUEST, "AMOUNT_TOO_SMALL");
        }
        validateAmount(provider, amount, currency);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable.", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        TransferRequest request = new TransferRequest();
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setPaymentMethod(providerCode);
        request.setBeneficiaryAccount(beneficiaryAccount);
        request.setBeneficiaryName(beneficiaryName);
        request.setDescription(description);

        return doTransfer(user, null, request);
    }

    private TransferResponse doTransfer(User user, Application application, TransferRequest request) {
        PaymentProvider provider = resolveProvider(request.getPaymentMethod());

        validateAmount(provider, request.getAmount(), request.getCurrency());

        long feeAmount  = feeCalculatorService.computeFee(provider, request.getAmount());
        long totalDebit = request.getAmount() + feeAmount;

        int updated = userBalanceRepository.moveAvailableToPending(
                user.getId(), request.getCurrency(), totalDebit);
        if (updated == 0) {
            throw new BusinessException("Solde disponible insuffisant.", HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE");
        }

        String reference = generateReference("PO");

        TransactionOut tx = TransactionOut.builder()
                .reference(reference)
                .user(user)
                .application(application)
                .paymentProvider(provider)
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .feeAmount(feeAmount)
                .netAmount(totalDebit)
                .description(request.getDescription())
                .merchantReference(request.getMerchantReference())
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryEmail(request.getBeneficiaryEmail())
                .beneficiaryAccount(request.getBeneficiaryAccount())
                .status(TransactionStatus.PENDING)
                .build();

        transactionOutRepository.save(tx);

        GatewayPayOutResponse gwResponse = gatewayRegistry.resolve(provider.getCode())
                .initiatePayOut(GatewayPayOutRequest.builder()
                        .reference(reference)
                        .providerRef(tx.getId().toString())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .beneficiaryAccount(request.getBeneficiaryAccount())
                        .beneficiaryName(request.getBeneficiaryName())
                        .description(request.getDescription())
                        .build());

        tx.setProviderTransactionId(gwResponse.getProviderTransactionId());
        transactionOutRepository.save(tx);

        if (application != null) {
            webhookService.dispatchEvent(application, "payout.created", payOutData(tx));
        }

        log.info("Payout créé : {} (user: {}, app: {}, provider: {}, montant: {} {}, providerTxId: {})",
                reference, user.getId(),
                application != null ? application.getId() : "—",
                provider.getCode(), request.getAmount(), request.getCurrency(),
                gwResponse.getProviderTransactionId());

        return TransferResponse.builder()
                .reference(reference)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .paymentMethod(provider.getCode())
                .beneficiaryAccount(request.getBeneficiaryAccount())
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryEmail(request.getBeneficiaryEmail())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PayOutStatusResponse checkStatus(UUID apiKeyId, String reference) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Clé API introuvable.", HttpStatus.UNAUTHORIZED, "API_KEY_INVALID"));

        TransactionOut tx = transactionOutRepository
                .findByReferenceAndApplication_Id(reference, apiKey.getApplication().getId())
                .orElseThrow(() -> new BusinessException("Payout introuvable.", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND"));

        return toStatusResponse(tx);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private PaymentProvider resolveProvider(String code) {
        return paymentProviderRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Moyen de paiement '" + code + "' introuvable.", HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND"));
    }

    private void validateAmount(PaymentProvider provider, Long amount, String currency) {
        if (!provider.getCurrency().equalsIgnoreCase(currency))
            throw new BusinessException("Devise incompatible avec ce provider.", HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH");
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
        return m;
    }

    private PayOutStatusResponse toStatusResponse(TransactionOut tx) {
        return PayOutStatusResponse.builder()
                .reference(tx.getReference())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .description(tx.getDescription())
                .paymentMethod(tx.getPaymentProvider() != null ? tx.getPaymentProvider().getCode() : null)
                .beneficiaryAccount(tx.getBeneficiaryAccount())
                .beneficiaryName(tx.getBeneficiaryName())
                .beneficiaryEmail(tx.getBeneficiaryEmail())
                .failureCode(tx.getFailureCode())
                .failureReason(tx.getFailureReason())
                .build();
    }
}
