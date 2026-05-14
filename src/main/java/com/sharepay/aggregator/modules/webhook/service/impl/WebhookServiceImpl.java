package com.sharepay.aggregator.modules.webhook.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.modules.webhook.dto.request.TestWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.request.UpdateWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.TestWebhookResponse;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookConfigResponse;
import com.sharepay.aggregator.modules.webhook.model.WebhookDelivery;
import com.sharepay.aggregator.modules.webhook.scheduler.WebhookDispatcher;
import com.sharepay.aggregator.modules.webhook.scheduler.WebhookPersistenceHelper;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final WebhookPersistenceHelper persistenceHelper;
    private final WebhookDispatcher dispatcher;

    // ─────────────────────────────────────────────────────────────────────────
    // GET CONFIG
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public WebhookConfigResponse getConfig(UUID apiKeyId) {
        return toResponse(resolveApplication(apiKeyId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE CONFIG
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public WebhookConfigResponse updateConfig(UUID apiKeyId, UpdateWebhookRequest request) {
        Application app = resolveApplication(apiKeyId);
        app.setWebhookUrl(request.getWebhookUrl());
        applicationRepository.save(app);
        log.info("Webhook URL mise à jour pour l'application '{}'", app.getName());
        return toResponse(app);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST WEBHOOK
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TestWebhookResponse testWebhook(UUID apiKeyId, TestWebhookRequest request) {
        return testWebhookForApp(resolveApplication(apiKeyId), request);
    }

    @Override
    public TestWebhookResponse testWebhookForApp(Application app, TestWebhookRequest request) {
        if (app.getWebhookUrl() == null || app.getWebhookUrl().isBlank()) {
            throw new BusinessException(
                    "Aucune URL webhook configurée pour cette application.",
                    HttpStatus.CONFLICT,
                    "WEBHOOK_URL_NOT_CONFIGURED"
            );
        }

        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> data = (request != null && request.getData() != null)
                ? request.getData()
                : Map.of("message", "Ceci est un webhook de test Sharepay.");

        String payload = buildPayload("webhook.test", app.getId(), timestamp, data);
        OffsetDateTime sentAt = OffsetDateTime.now();

        try {
            var requestSpec = restClient.post()
                    .uri(app.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Sharepay-Event", "webhook.test");

            if (app.getWebhookSecret() != null) {
                requestSpec = requestSpec.header(
                        "X-Sharepay-Signature",
                        "t=" + timestamp + ",v1=" + dispatcher.sign(payload, app.getWebhookSecret())
                );
            }

            ResponseEntity<Void> response = requestSpec.body(payload).retrieve().toBodilessEntity();

            log.info("Webhook de test envoyé avec succès à '{}' pour l'application '{}'",
                    app.getWebhookUrl(), app.getName());

            return TestWebhookResponse.builder()
                    .delivered(true)
                    .webhookUrl(app.getWebhookUrl())
                    .httpStatus(response.getStatusCode().value())
                    .sentAt(sentAt)
                    .build();

        } catch (RestClientResponseException e) {
            log.warn("Webhook de test rejeté par '{}' (HTTP {}) pour l'application '{}'",
                    app.getWebhookUrl(), e.getStatusCode().value(), app.getName());
            return TestWebhookResponse.builder()
                    .delivered(false)
                    .webhookUrl(app.getWebhookUrl())
                    .httpStatus(e.getStatusCode().value())
                    .sentAt(sentAt)
                    .build();

        } catch (Exception e) {
            log.warn("Échec du webhook de test pour l'application '{}' → {} : {}",
                    app.getName(), app.getWebhookUrl(), e.getMessage());
            throw new BusinessException(
                    "Le webhook de test a échoué. Vérifiez que l'URL est accessible. Détail : " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY,
                    "WEBHOOK_TEST_FAILED"
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DISPATCH (async + persistance)
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    @Override
    public void dispatchEvent(Application app, String eventName, Map<String, Object> data) {
        if (app.getWebhookUrl() == null || app.getWebhookUrl().isBlank()) return;

        // Capture des champs avant la frontière async — l'entité peut être détachée ensuite
        UUID appId        = app.getId();
        String webhookUrl = app.getWebhookUrl();
        String secret     = app.getWebhookSecret();

        long timestamp = Instant.now().getEpochSecond();
        String payload = buildPayload(eventName, appId, timestamp, data);
        if (payload == null) return;

        // Transaction courte : persister PENDING
        WebhookDelivery delivery = persistenceHelper.createPending(appId, eventName, payload);

        // Appel réseau hors transaction, mise à jour du statut via dispatcher
        dispatcher.sendAndRecord(delivery.getId(), webhookUrl, secret, eventName, timestamp, payload);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private Application resolveApplication(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException(
                        "Clé API introuvable.", HttpStatus.UNAUTHORIZED, "API_KEY_INVALID"
                ));
        return apiKey.getApplication();
    }

    private String buildPayload(String eventName, UUID appId, long timestamp, Map<String, Object> data) {
        try {
            String dataJson = objectMapper.writeValueAsString(data);
            return """
                    {
                      "event": "%s",
                      "timestamp": %d,
                      "applicationId": "%s",
                      "data": %s
                    }
                    """.formatted(eventName, timestamp, appId, dataJson);
        } catch (JsonProcessingException e) {
            log.error("Échec sérialisation payload webhook '{}' pour app '{}' : {}", eventName, appId, e.getMessage());
            return null;
        }
    }

    private WebhookConfigResponse toResponse(Application app) {
        String secretPrefix = app.getWebhookSecret() != null && app.getWebhookSecret().length() > 14
                ? app.getWebhookSecret().substring(0, 14) + "..."
                : "whsec_...";

        return WebhookConfigResponse.builder()
                .applicationId(app.getId())
                .applicationName(app.getName())
                .webhookUrl(app.getWebhookUrl())
                .webhookSecretPrefix(secretPrefix)
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
