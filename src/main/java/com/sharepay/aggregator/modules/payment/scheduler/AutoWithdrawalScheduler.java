package com.sharepay.aggregator.modules.payment.scheduler;

import com.sharepay.aggregator.modules.account.model.WithdrawalConfig;
import com.sharepay.aggregator.modules.account.repository.WithdrawalConfigRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoWithdrawalScheduler {

    private final WithdrawalConfigRepository configRepository;
    private final UserBalanceRepository      userBalanceRepository;
    private final PayOutService              payOutService;
    private final WithdrawalCircuitBreaker   circuitBreaker;

    /** Vérifie les configs THRESHOLD toutes les 5 minutes. */
    @Scheduled(fixedDelay = 300_000)
    public void checkThreshold() {
        var configs = configRepository.findActiveByMode(WithdrawalMode.THRESHOLD);
        if (configs.isEmpty()) return;

        log.info("[AutoWithdrawal] THRESHOLD — {} config(s) à vérifier", configs.size());
        configs.forEach(this::triggerIfThresholdReached);
    }

    /** Vérifie les configs PERIODIC toutes les heures. */
    @Scheduled(cron = "0 0 * * * *")
    public void checkPeriodic() {
        var configs = configRepository.findActiveByMode(WithdrawalMode.PERIODIC);
        if (configs.isEmpty()) return;

        log.info("[AutoWithdrawal] PERIODIC — {} config(s) à vérifier", configs.size());
        configs.forEach(this::triggerIfPeriodElapsed);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pas de @Transactional ici : chaque appel délégué gère sa propre transaction.
    // Cela évite que l'échec d'un retrait marque la transaction englobante en
    // rollback-only et empêche l'enregistrement de l'erreur dans le circuit breaker.

    protected void triggerIfThresholdReached(WithdrawalConfig config) {
        var userId   = config.getUser().getId();
        var currency = config.getCurrency();

        var balance = userBalanceRepository.findByUser_IdAndCurrency(userId, currency).orElse(null);
        if (balance == null || balance.getAvailableAmount() < config.getThresholdAmount()) return;

        long amount = balance.getAvailableAmount();

        try {
            String ref = executeAutoWithdrawal(config, amount, currency, "Retrait automatique par seuil");
            circuitBreaker.recordSuccess(config.getId(), null);
            log.info("[AutoWithdrawal] THRESHOLD déclenché — user: {}, ref: {}, gross: {} {}",
                    userId, ref, amount, currency);
        } catch (BusinessException e) {
            if ("AMOUNT_BELOW_MINIMUM".equals(e.getCode()) || "AMOUNT_TOO_SMALL".equals(e.getCode())) {
                log.warn("[AutoWithdrawal] THRESHOLD — montant net après frais trop faible ({} {}), ignoré", amount, currency);
                return;
            }
            circuitBreaker.recordFailure(config.getId(), e.getMessage());
            log.error("[AutoWithdrawal] THRESHOLD échoué — user: {} — [{}] {}", userId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            circuitBreaker.recordFailure(config.getId(), e.getMessage());
            log.error("[AutoWithdrawal] THRESHOLD erreur inattendue — user: {} — {}", userId, e.getMessage());
        }
    }

    protected void triggerIfPeriodElapsed(WithdrawalConfig config) {
        if (config.getPeriod() == null) return;

        OffsetDateTime now  = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime last = config.getLastTriggeredAt();

        boolean shouldTrigger = switch (config.getPeriod()) {
            case DAILY   -> last == null || last.toLocalDate().isBefore(now.toLocalDate());
            case WEEKLY  -> last == null || last.toLocalDate()
                                .with(DayOfWeek.MONDAY)
                                .isBefore(now.toLocalDate().with(DayOfWeek.MONDAY));
            case MONTHLY -> last == null || last.toLocalDate().withDayOfMonth(1)
                                .isBefore(now.toLocalDate().withDayOfMonth(1));
        };

        if (!shouldTrigger) return;

        var userId   = config.getUser().getId();
        var currency = config.getCurrency();

        var balance = userBalanceRepository.findByUser_IdAndCurrency(userId, currency).orElse(null);
        if (balance == null || balance.getAvailableAmount() <= 0) return;

        long amount = balance.getAvailableAmount();

        try {
            String ref = executeAutoWithdrawal(config, amount, currency, "Retrait automatique périodique");
            circuitBreaker.recordSuccess(config.getId(), now);
            log.info("[AutoWithdrawal] PERIODIC ({}) déclenché — user: {}, ref: {}, gross: {} {}",
                    config.getPeriod(), userId, ref, amount, currency);
        } catch (BusinessException e) {
            if ("AMOUNT_BELOW_MINIMUM".equals(e.getCode()) || "AMOUNT_TOO_SMALL".equals(e.getCode())) {
                log.warn("[AutoWithdrawal] PERIODIC — montant net après frais trop faible ({} {}), ignoré", amount, currency);
                return;
            }
            circuitBreaker.recordFailure(config.getId(), e.getMessage());
            log.error("[AutoWithdrawal] PERIODIC échoué — user: {} — [{}] {}", userId, e.getCode(), e.getMessage());
        } catch (Exception e) {
            circuitBreaker.recordFailure(config.getId(), e.getMessage());
            log.error("[AutoWithdrawal] PERIODIC erreur inattendue — user: {} — {}", userId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String executeAutoWithdrawal(WithdrawalConfig config, long gross, String currency, String description) {
        var account = config.getAccount();
        return payOutService.createAutoWithdrawal(
                config.getUser().getId(),
                account.getProviderCode(),
                account.getAccountNumber(),
                account.getAccountName(),
                gross,
                currency,
                description
        ).getReference();
    }
}
