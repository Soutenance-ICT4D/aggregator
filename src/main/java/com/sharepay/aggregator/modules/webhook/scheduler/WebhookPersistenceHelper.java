package com.sharepay.aggregator.modules.webhook.scheduler;

import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.modules.webhook.constant.WebhookDeliveryStatus;
import com.sharepay.aggregator.modules.webhook.model.WebhookDelivery;
import com.sharepay.aggregator.modules.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Encapsule les opérations de persistance webhook dans des transactions courtes,
 * séparées des appels réseau — même pattern que TransactionPersistenceHelper.
 */
@Component
@RequiredArgsConstructor
public class WebhookPersistenceHelper {

    private static final int MAX_ATTEMPTS = 5;

    private final WebhookDeliveryRepository deliveryRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public WebhookDelivery createPending(UUID applicationId, String eventName, String payload) {
        Application app = applicationRepository.getReferenceById(applicationId);
        WebhookDelivery delivery = WebhookDelivery.builder()
                .application(app)
                .eventName(eventName)
                .payload(payload)
                .build();
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void markDelivered(UUID deliveryId, int httpStatus) {
        deliveryRepository.findById(deliveryId).ifPresent(d -> {
            d.setStatus(WebhookDeliveryStatus.DELIVERED);
            d.setHttpStatus(httpStatus);
            d.setAttemptCount(d.getAttemptCount() + 1);
            d.setNextRetryAt(null);
            deliveryRepository.save(d);
        });
    }

    @Transactional
    public void markFailed(UUID deliveryId, Integer httpStatus, String error) {
        deliveryRepository.findById(deliveryId).ifPresent(d -> {
            int attempts = d.getAttemptCount() + 1;
            d.setStatus(WebhookDeliveryStatus.FAILED);
            d.setHttpStatus(httpStatus);
            d.setAttemptCount(attempts);
            d.setLastError(error);
            d.setNextRetryAt(attempts < MAX_ATTEMPTS ? nextRetryAt(attempts) : null);
            deliveryRepository.save(d);
        });
    }

    @Transactional(readOnly = true)
    public List<WebhookDelivery> loadRetryable() {
        return deliveryRepository.findRetryable(OffsetDateTime.now(), MAX_ATTEMPTS);
    }

    /** Backoff exponentiel : 1min → 5min → 30min → 2h → abandon */
    private OffsetDateTime nextRetryAt(int attemptsDone) {
        return switch (attemptsDone) {
            case 1 -> OffsetDateTime.now().plusMinutes(1);
            case 2 -> OffsetDateTime.now().plusMinutes(5);
            case 3 -> OffsetDateTime.now().plusMinutes(30);
            default -> OffsetDateTime.now().plusHours(2);
        };
    }
}
