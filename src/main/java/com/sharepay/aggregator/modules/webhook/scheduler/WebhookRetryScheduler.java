package com.sharepay.aggregator.modules.webhook.scheduler;

import com.sharepay.aggregator.modules.webhook.model.WebhookDelivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final WebhookPersistenceHelper persistenceHelper;
    private final WebhookDispatcher dispatcher;

    /**
     * Toutes les 2 minutes : retrouve les livraisons FAILED éligibles au retry
     * et tente de les renvoyer.
     * Backoff : 1min → 5min → 30min → 2h → abandon définitif après 5 tentatives.
     */
    @Scheduled(fixedDelay = 2 * 60 * 1000)
    public void retryFailedDeliveries() {
        List<WebhookDelivery> retryable = persistenceHelper.loadRetryable();

        if (retryable.isEmpty()) return;

        log.info("[WebhookRetry] {} livraison(s) à retenter", retryable.size());

        retryable.forEach(delivery -> {
            try {
                String webhookUrl = delivery.getApplication().getWebhookUrl();
                String secret     = delivery.getApplication().getWebhookSecret();

                if (webhookUrl == null || webhookUrl.isBlank()) {
                    persistenceHelper.markFailed(delivery.getId(), null, "URL webhook supprimée depuis l'échec initial.");
                    return;
                }

                long timestamp = Instant.now().getEpochSecond();

                // Mettre à jour le timestamp dans le payload stocké pour rester cohérent
                String payload = delivery.getPayload().replaceFirst(
                        "\"timestamp\":\\s*\\d+",
                        "\"timestamp\": " + timestamp
                );

                dispatcher.sendAndRecord(delivery.getId(), webhookUrl, secret,
                        delivery.getEventName(), timestamp, payload);

            } catch (Exception e) {
                log.error("[WebhookRetry] Erreur inattendue sur delivery {} : {}", delivery.getId(), e.getMessage());
            }
        });
    }
}
