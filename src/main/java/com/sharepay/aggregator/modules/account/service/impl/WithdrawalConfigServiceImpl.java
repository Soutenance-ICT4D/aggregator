package com.sharepay.aggregator.modules.account.service.impl;

import com.sharepay.aggregator.modules.account.dto.request.CreateWithdrawalAccountRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateWithdrawalConfigRequest;
import com.sharepay.aggregator.modules.account.dto.response.WithdrawalAccountResponse;
import com.sharepay.aggregator.modules.account.dto.response.WithdrawalConfigResponse;
import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.model.WithdrawalAccount;
import com.sharepay.aggregator.modules.account.model.WithdrawalConfig;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.account.repository.WithdrawalAccountRepository;
import com.sharepay.aggregator.modules.account.repository.WithdrawalConfigRepository;
import com.sharepay.aggregator.modules.account.service.WithdrawalConfigService;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WithdrawalConfigServiceImpl implements WithdrawalConfigService {

    private final WithdrawalAccountRepository accountRepository;
    private final WithdrawalConfigRepository  configRepository;
    private final UserRepository              userRepository;
    private final PaymentProviderRepository   paymentProviderRepository;

    // ── Comptes ───────────────────────────────────────────────────────────────

    @Override
    public List<WithdrawalAccountResponse> getAccounts(UUID userId) {
        return accountRepository.findByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtAsc(userId)
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Override
    @Transactional
    public WithdrawalAccountResponse addAccount(UUID userId, CreateWithdrawalAccountRequest request) {
        // Vérifier que le provider existe
        var provider = paymentProviderRepository.findByCode(request.getProviderCode())
                .orElseThrow(() -> new BusinessException(
                        "Opérateur '" + request.getProviderCode() + "' introuvable.",
                        HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND"));

        if (!provider.isActive()) {
            throw new BusinessException("Opérateur indisponible.", HttpStatus.BAD_REQUEST, "PROVIDER_INACTIVE");
        }

        User user = userRepository.getReferenceById(userId);

        // Si ce compte doit être le défaut, effacer les anciens
        if (request.isDefault()) {
            accountRepository.clearDefaultForUser(userId);
        }

        // Si c'est le premier compte actif, le mettre par défaut automatiquement
        boolean firstAccount = accountRepository.findByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtAsc(userId).isEmpty();

        WithdrawalAccount account = WithdrawalAccount.builder()
                .user(user)
                .providerCode(request.getProviderCode())
                .providerName(provider.getName())
                .providerType(provider.getType().name())
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .isDefault(request.isDefault() || firstAccount)
                .build();

        return toAccountResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId, UUID accountId) {
        WithdrawalAccount account = accountRepository.findByIdAndUser_IdAndDeletedAtIsNull(accountId, userId)
                .orElseThrow(() -> new BusinessException(
                        "Compte introuvable.", HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND"));

        boolean wasDefault = account.isDefault();

        // Soft delete : marquer comme supprimé et retirer le statut par défaut
        account.setDeletedAt(OffsetDateTime.now());
        account.setDefault(false);
        accountRepository.save(account);

        // Si c'était le compte par défaut, promouvoir le plus ancien compte actif restant
        if (wasDefault) {
            accountRepository.findByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtAsc(userId)
                    .stream().findFirst().ifPresent(next -> {
                        next.setDefault(true);
                        accountRepository.save(next);
                    });
        }

        // Détacher du config si ce compte était configuré
        configRepository.findByUserId(userId).ifPresent(cfg -> {
            if (cfg.getAccount() != null && cfg.getAccount().getId().equals(accountId)) {
                cfg.setAccount(null);
                if (cfg.getMode() != WithdrawalMode.MANUAL) {
                    cfg.setMode(WithdrawalMode.MANUAL);
                    log.warn("Config repassée en MANUAL après soft-delete du compte cible (user {})", userId);
                }
                configRepository.save(cfg);
            }
        });
    }

    @Override
    @Transactional
    public WithdrawalAccountResponse setDefaultAccount(UUID userId, UUID accountId) {
        WithdrawalAccount account = accountRepository.findByIdAndUser_IdAndDeletedAtIsNull(accountId, userId)
                .orElseThrow(() -> new BusinessException(
                        "Compte introuvable.", HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND"));

        accountRepository.clearDefaultForUser(userId);
        account.setDefault(true);
        return toAccountResponse(accountRepository.save(account));
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    @Override
    public WithdrawalConfigResponse getConfig(UUID userId) {
        return configRepository.findByUserId(userId)
                .map(this::toConfigResponse)
                .orElse(WithdrawalConfigResponse.builder()
                        .mode(WithdrawalMode.MANUAL)
                        .currency("XAF")
                        .build());
    }

    @Override
    @Transactional
    public WithdrawalConfigResponse updateConfig(UUID userId, UpdateWithdrawalConfigRequest request) {
        validateConfigRequest(userId, request);

        WithdrawalConfig config = configRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.getReferenceById(userId);
            return WithdrawalConfig.builder().user(user).build();
        });

        config.setMode(request.getMode());
        config.setThresholdAmount(null);
        config.setPeriod(null);
        config.setAccount(null);

        if (request.getMode() != WithdrawalMode.MANUAL && request.getAccountId() != null) {
            WithdrawalAccount account = accountRepository.findByIdAndUser_IdAndDeletedAtIsNull(request.getAccountId(), userId)
                    .orElseThrow(() -> new BusinessException(
                            "Compte introuvable.", HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND"));
            config.setAccount(account);
        }

        switch (request.getMode()) {
            case THRESHOLD -> config.setThresholdAmount(request.getThresholdAmount());
            case PERIODIC  -> config.setPeriod(request.getPeriod());
            default        -> {}
        }

        return toConfigResponse(configRepository.save(config));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateConfigRequest(UUID userId, UpdateWithdrawalConfigRequest request) {
        switch (request.getMode()) {
            case INSTANT, THRESHOLD, PERIODIC -> {
                if (request.getAccountId() == null) {
                    throw new BusinessException(
                            "Un compte de retrait est requis pour ce mode.",
                            HttpStatus.BAD_REQUEST, "ACCOUNT_REQUIRED");
                }
            }
            default -> {}
        }
        if (request.getMode() == WithdrawalMode.THRESHOLD && request.getThresholdAmount() == null) {
            throw new BusinessException(
                    "Le montant seuil est requis pour le mode THRESHOLD.",
                    HttpStatus.BAD_REQUEST, "THRESHOLD_AMOUNT_REQUIRED");
        }
        if (request.getMode() == WithdrawalMode.PERIODIC && request.getPeriod() == null) {
            throw new BusinessException(
                    "La périodicité est requise pour le mode PERIODIC.",
                    HttpStatus.BAD_REQUEST, "PERIOD_REQUIRED");
        }
    }

    WithdrawalAccountResponse toAccountResponse(WithdrawalAccount a) {
        return WithdrawalAccountResponse.builder()
                .id(a.getId())
                .providerCode(a.getProviderCode())
                .providerName(a.getProviderName())
                .providerType(a.getProviderType())
                .accountNumber(a.getAccountNumber())
                .accountName(a.getAccountName())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private WithdrawalConfigResponse toConfigResponse(WithdrawalConfig c) {
        return WithdrawalConfigResponse.builder()
                .mode(c.getMode())
                .account(c.getAccount() != null ? toAccountResponse(c.getAccount()) : null)
                .thresholdAmount(c.getThresholdAmount())
                .period(c.getPeriod())
                .currency(c.getCurrency())
                .lastTriggeredAt(c.getLastTriggeredAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
