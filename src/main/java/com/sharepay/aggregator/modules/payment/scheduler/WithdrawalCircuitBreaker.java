package com.sharepay.aggregator.modules.payment.scheduler;

import com.sharepay.aggregator.modules.account.repository.WithdrawalConfigRepository;
import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Enregistre les succès/échecs des retraits automatiques dans leur propre transaction (REQUIRES_NEW),
 * isolée de la transaction du retrait elle-même. Après MAX_FAILURES échecs consécutifs,
 * la config est repassée en MANUAL pour éviter des boucles d'erreur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCircuitBreaker {

    private static final int MAX_FAILURES = 3;

    private final WithdrawalConfigRepository configRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID configId, OffsetDateTime lastTriggeredAt) {
        configRepository.findById(configId).ifPresent(cfg -> {
            cfg.setConsecutiveFailures(0);
            cfg.setLastErrorAt(null);
            cfg.setLastErrorMessage(null);
            if (lastTriggeredAt != null) {
                cfg.setLastTriggeredAt(lastTriggeredAt);
            }
            configRepository.save(cfg);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID configId, String errorMessage) {
        configRepository.findById(configId).ifPresent(cfg -> {
            int failures = cfg.getConsecutiveFailures() + 1;
            cfg.setConsecutiveFailures(failures);
            cfg.setLastErrorAt(OffsetDateTime.now());
            cfg.setLastErrorMessage(truncate(errorMessage, 500));

            if (failures >= MAX_FAILURES) {
                log.warn("[CircuitBreaker] Config {} ({}) - {} échecs consécutifs → repassage en MANUAL",
                        configId, cfg.getMode(), failures);
                cfg.setMode(WithdrawalMode.MANUAL);
                cfg.setAccount(null);
                cfg.setThresholdAmount(null);
                cfg.setPeriod(null);
            }

            configRepository.save(cfg);
        });
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
