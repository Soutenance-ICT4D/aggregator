package com.sharepay.aggregator.modules.webhook.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Responsable de l'envoi HTTP d'un webhook et de la mise à jour de son statut.
 * Partagé par WebhookServiceImpl (dispatch initial) et WebhookRetryScheduler (retry).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final RestClient restClient;
    private final WebhookPersistenceHelper persistenceHelper;

    public void sendAndRecord(UUID deliveryId, String webhookUrl, String secret,
                              String eventName, long timestamp, String payload) {
        try {
            var req = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Sharepay-Event", eventName);

            if (secret != null) {
                req = req.header("X-Sharepay-Signature", "t=" + timestamp + ",v1=" + sign(payload, secret));
            }

            ResponseEntity<Void> response = req.body(payload).retrieve().toBodilessEntity();
            int httpStatus = response.getStatusCode().value();

            persistenceHelper.markDelivered(deliveryId, httpStatus);
            log.info("Webhook '{}' livré à '{}' (HTTP {}, signé: {})", eventName, webhookUrl, httpStatus, secret != null);

        } catch (RestClientResponseException e) {
            int httpStatus = e.getStatusCode().value();
            persistenceHelper.markFailed(deliveryId, httpStatus, "HTTP " + httpStatus + " : " + e.getMessage());
            log.warn("Webhook '{}' rejeté par '{}' (HTTP {})", eventName, webhookUrl, httpStatus);

        } catch (Exception e) {
            persistenceHelper.markFailed(deliveryId, null, e.getMessage());
            log.warn("Webhook '{}' non livré à '{}' : {}", eventName, webhookUrl, e.getMessage());
        }
    }

    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la signature du webhook", e);
        }
    }
}
