package com.sharepay.aggregator.modules.collect.scheduler;

import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.modules.collect.repository.FundCollectionRepository;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FundCollectionExpiryScheduler {

    private final FundCollectionRepository fundCollectionRepository;
    private final WebhookService webhookService;

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

        expired.forEach(c ->
                webhookService.dispatchEvent(c.getApplication(), "collection.expired", collectionData(c)));
    }

    private Map<String, Object> collectionData(FundCollection c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          c.getId());
        m.put("slug",        c.getSlug());
        m.put("title",       c.getTitle());
        m.put("status",      c.getStatus().name());
        m.put("currency",    c.getCurrency());
        m.put("amount",      c.getAmount());
        m.put("expiresAt",   c.getExpiresAt());
        m.put("updatedAt",   c.getUpdatedAt());
        return m;
    }
}
