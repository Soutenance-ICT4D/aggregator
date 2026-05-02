package com.sharepay.aggregator.modules.webhook.service.impl;

import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharepay.aggregator.modules.webhook.dto.request.TestWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.request.UpdateWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.TestWebhookResponse;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookConfigResponse;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

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
        Application app = resolveApplication(apiKeyId);

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

        String payload = buildTestPayload(app.getId(), timestamp, data);
        String signature = sign(payload, app.getWebhookSecret());
        OffsetDateTime sentAt = OffsetDateTime.now();

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(app.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Sharepay-Signature", "t=" + timestamp + ",v1=" + signature)
                    .header("X-Sharepay-Event", "webhook.test")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

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
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private Application resolveApplication(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new BusinessException(
                        "Clé API introuvable.",
                        HttpStatus.UNAUTHORIZED,
                        "API_KEY_INVALID"
                ));
        return apiKey.getApplication();
    }

    private String buildTestPayload(UUID appId, long timestamp, Map<String, Object> data) {
        try {
            String dataJson = objectMapper.writeValueAsString(data);
            return """
                    {
                      "event": "webhook.test",
                      "timestamp": %d,
                      "applicationId": "%s",
                      "data": %s
                    }
                    """.formatted(timestamp, appId, dataJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la sérialisation du payload webhook", e);
        }
    }

    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la signature du webhook", e);
        }
    }

    @Override
    public void dispatchEvent(Application app, String eventName, Map<String, Object> data) {
        if (app.getWebhookUrl() == null || app.getWebhookUrl().isBlank()) return;

        long timestamp = Instant.now().getEpochSecond();
        try {
            String dataJson = objectMapper.writeValueAsString(data);
            String payload = """
                    {
                      "event": "%s",
                      "timestamp": %d,
                      "applicationId": "%s",
                      "data": %s
                    }
                    """.formatted(eventName, timestamp, app.getId(), dataJson);
            String signature = sign(payload, app.getWebhookSecret());

            restClient.post()
                    .uri(app.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Sharepay-Signature", "t=" + timestamp + ",v1=" + signature)
                    .header("X-Sharepay-Event", eventName)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Webhook '{}' envoyé à '{}' (app: {})", eventName, app.getWebhookUrl(), app.getId());
        } catch (Exception e) {
            log.warn("Échec webhook '{}' pour app '{}' → {}", eventName, app.getId(), e.getMessage());
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
