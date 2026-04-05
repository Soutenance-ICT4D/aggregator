package com.sharepay.aggregator.modules.collect.scheduler;

import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.modules.collect.repository.FundCollectionRepository;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundCollectionExpiryScheduler {

    private final FundCollectionRepository fundCollectionRepository;

    /**
     * Vérifie toutes les 15 minutes les collectes ACTIVE dont la date d'expiration est dépassée
     * et les passe automatiquement au statut EXPIRED.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
    public void expireOverdueCollections() {
        List<FundCollection> expired = fundCollectionRepository
                .findByStatusAndExpiresAtBefore(FundCollectionStatus.ACTIVE, OffsetDateTime.now());

        if (expired.isEmpty()) return;

        expired.forEach(c -> c.setStatus(FundCollectionStatus.EXPIRED));
        fundCollectionRepository.saveAll(expired);

        log.info("[Scheduler] {} collecte(s) passée(s) au statut EXPIRED.", expired.size());
    }
}
