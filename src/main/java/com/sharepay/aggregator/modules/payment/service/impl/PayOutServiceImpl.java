package com.sharepay.aggregator.modules.payment.service.impl;

import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.shared.constant.AppStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOutServiceImpl implements PayOutService {

    private final TransactionOutRepository transactionOutRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final FeeCalculatorService feeCalculatorService;

    private final SecureRandom secureRandom = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────────
    // TRANSFER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse createTransfer(UUID apiKeyId, TransferRequest request) {
        return doTransfer(resolveApplication(apiKeyId), request);
    }

    @Override
    @Transactional
    public TransferResponse createTransferByUserId(UUID userId, TransferRequest request) {
        Application application = applicationRepository
                .findFirstByUser_IdAndStatusNotOrderByCreatedAtDesc(userId, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException("Aucune application active.", HttpStatus.BAD_REQUEST, "NO_APPLICATION"));
        return doTransfer(application, request);
    }

    private TransferResponse doTransfer(Application application, TransferRequest request) {
        PaymentProvider provider = resolveProvider(request.getPaymentMethod());

        validateAmount(provider, request.getAmount(), request.getCurrency());

        long feeAmount  = feeCalculatorService.computeFee(provider, request.getAmount());
        long totalDebit = request.getAmount() + feeAmount;

        int updated = userBalanceRepository.moveAvailableToPending(
                application.getUser().getId(), request.getCurrency(), totalDebit);
        if (updated == 0) {
            throw new BusinessException("Solde disponible insuffisant.", HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE");
        }

        String reference = generateReference("PO");

        TransactionOut tx = TransactionOut.builder()
                .reference(reference)
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

        log.info("Payout créé : {} (app: {}, provider: {}, montant: {} {}, providerTxId: {})",
                reference, application.getId(), provider.getCode(),
                request.getAmount(), request.getCurrency(), gwResponse.getProviderTransactionId());

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
        Application application = resolveApplication(apiKeyId);

        TransactionOut tx = transactionOutRepository
                .findByReferenceAndApplication_Id(reference, application.getId())
                .orElseThrow(() -> new BusinessException("Payout introuvable.", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND"));

        return toStatusResponse(tx);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private Application resolveApplication(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException("Clé API introuvable.", HttpStatus.UNAUTHORIZED, "API_KEY_INVALID"));
        return apiKey.getApplication();
    }

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
