package com.sharepay.aggregator.modules.apps.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.apps.dto.request.UpdateAppWebhookRequest;
import com.sharepay.aggregator.modules.apps.dto.response.AppWebhookResponse;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.modules.apps.service.AppWebhookService;
import com.sharepay.aggregator.modules.webhook.constant.WebhookDeliveryStatus;
import com.sharepay.aggregator.modules.webhook.dto.request.TestWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.TestWebhookResponse;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookDeliveryResponse;
import com.sharepay.aggregator.modules.webhook.model.WebhookDelivery;
import com.sharepay.aggregator.modules.webhook.repository.WebhookDeliveryRepository;
import com.sharepay.aggregator.modules.webhook.scheduler.WebhookDispatcher;
import com.sharepay.aggregator.modules.webhook.scheduler.WebhookPersistenceHelper;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.constant.AppStatus;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppWebhookServiceImpl implements AppWebhookService {

    private static final String WEBHOOK_SECRET_PREFIX = "whsec_";
    private static final int WEBHOOK_SECRET_BYTES = 32;

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WebhookService webhookService;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookPersistenceHelper persistenceHelper;
    private final WebhookDispatcher dispatcher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public AppWebhookResponse getWebhookConfig(UUID userId, UUID appId) {
        return toResponse(resolveApplication(userId, appId), null);
    }

    @Override
    @Transactional
    public AppWebhookResponse updateWebhookUrl(UUID userId, UUID appId, UpdateAppWebhookRequest request) {
        Application app = resolveApplication(userId, appId);

        String newUrl = (request.getWebhookUrl() != null && !request.getWebhookUrl().isBlank())
                ? request.getWebhookUrl()
                : null;
        app.setWebhookUrl(newUrl);
        applicationRepository.save(app);

        log.info("Webhook URL mise à jour pour l'application '{}' (user {})", app.getName(), userId);
        return toResponse(app, null);
    }

    @Override
    @Transactional
    public AppWebhookResponse createSecret(UUID userId, UUID appId) {
        Application app = resolveApplication(userId, appId);

        if (app.getWebhookSecret() != null) {
            throw new BusinessException(
                    "Un webhook secret existe déjà pour cette application. Utilisez la rotation pour le remplacer.",
                    HttpStatus.CONFLICT,
                    "WEBHOOK_SECRET_ALREADY_EXISTS"
            );
        }

        String secret = generateWebhookSecret();
        app.setWebhookSecret(secret);
        applicationRepository.save(app);

        log.info("Webhook secret créé pour l'application '{}' (user {})", app.getName(), userId);
        return toResponse(app, secret);
    }

    @Override
    @Transactional
    public AppWebhookResponse rotateSecret(UUID userId, UUID appId) {
        Application app = resolveApplication(userId, appId);

        if (app.getWebhookSecret() == null) {
            throw new BusinessException(
                    "Aucun webhook secret configuré. Utilisez POST /secret pour en créer un.",
                    HttpStatus.CONFLICT,
                    "WEBHOOK_SECRET_NOT_FOUND"
            );
        }

        String secret = generateWebhookSecret();
        app.setWebhookSecret(secret);
        applicationRepository.save(app);

        log.info("Webhook secret renouvelé pour l'application '{}' (user {})", app.getName(), userId);
        return toResponse(app, secret);
    }

    @Override
    @Transactional
    public void revokeSecret(UUID userId, UUID appId) {
        Application app = resolveApplication(userId, appId);

        if (app.getWebhookSecret() == null) {
            throw new BusinessException(
                    "Aucun webhook secret à révoquer pour cette application.",
                    HttpStatus.NOT_FOUND,
                    "WEBHOOK_SECRET_NOT_FOUND"
            );
        }

        app.setWebhookSecret(null);
        applicationRepository.save(app);

        log.info("Webhook secret révoqué pour l'application '{}' (user {})", app.getName(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestWebhookResponse testWebhook(UUID userId, UUID appId, TestWebhookRequest request) {
        return webhookService.testWebhookForApp(resolveApplication(userId, appId), request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WebhookDeliveryResponse> listDeliveries(UUID userId, UUID appId,
                                                        WebhookDeliveryStatus status, Pageable pageable) {
        Application app = resolveApplication(userId, appId);
        Page<WebhookDelivery> page = status != null
                ? deliveryRepository.findByApplication_IdAndStatusOrderByCreatedAtDesc(app.getId(), status, pageable)
                : deliveryRepository.findByApplication_IdOrderByCreatedAtDesc(app.getId(), pageable);
        return page.map(this::toDeliveryResponse);
    }

    @Override
    public WebhookDeliveryResponse replayDelivery(UUID userId, UUID appId, UUID deliveryId) {
        Application app = resolveApplication(userId, appId);

        if (app.getWebhookUrl() == null || app.getWebhookUrl().isBlank()) {
            throw new BusinessException(
                    "Aucune URL webhook configurée pour cette application.",
                    HttpStatus.CONFLICT,
                    "WEBHOOK_URL_NOT_CONFIGURED"
            );
        }

        WebhookDelivery original = deliveryRepository.findByIdAndApplication_Id(deliveryId, app.getId())
                .orElseThrow(() -> new BusinessException(
                        "Livraison introuvable.",
                        HttpStatus.NOT_FOUND,
                        "DELIVERY_NOT_FOUND"
                ));

        WebhookDelivery replay = persistenceHelper.createPending(app.getId(),
                original.getEventName(), original.getPayload());

        dispatcher.sendAndRecord(replay.getId(), app.getWebhookUrl(), app.getWebhookSecret(),
                original.getEventName(), Instant.now().getEpochSecond(), original.getPayload());

        // Recharge après mise à jour du statut par le dispatcher
        return deliveryRepository.findById(replay.getId())
                .map(this::toDeliveryResponse)
                .orElseGet(() -> toDeliveryResponse(replay));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Application resolveApplication(UUID userId, UUID appId) {
        User user = userRepository.getReferenceById(userId);
        return applicationRepository
                .findByIdAndUserAndStatusNot(appId, user, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Application non trouvée.",
                        HttpStatus.NOT_FOUND,
                        "APP_NOT_FOUND"
                ));
    }

    private AppWebhookResponse toResponse(Application app, String plainTextSecret) {
        String secretPrefix = app.getWebhookSecret() != null && app.getWebhookSecret().length() > 14
                ? app.getWebhookSecret().substring(0, 14)
                : null;

        return AppWebhookResponse.builder()
                .applicationId(app.getId())
                .applicationName(app.getName())
                .webhookUrl(app.getWebhookUrl())
                .webhookSecretPrefix(secretPrefix)
                .plainTextWebhookSecret(plainTextSecret)
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[WEBHOOK_SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return WEBHOOK_SECRET_PREFIX + HexFormat.of().formatHex(bytes);
    }

    private WebhookDeliveryResponse toDeliveryResponse(WebhookDelivery d) {
        return WebhookDeliveryResponse.builder()
                .id(d.getId())
                .eventName(d.getEventName())
                .status(d.getStatus())
                .httpStatus(d.getHttpStatus())
                .attemptCount(d.getAttemptCount())
                .nextRetryAt(d.getNextRetryAt())
                .lastError(d.getLastError())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
