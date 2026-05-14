package com.sharepay.aggregator.modules.apps.service;

import com.sharepay.aggregator.modules.apps.dto.request.UpdateAppWebhookRequest;
import com.sharepay.aggregator.modules.apps.dto.response.AppWebhookResponse;
import com.sharepay.aggregator.modules.webhook.constant.WebhookDeliveryStatus;
import com.sharepay.aggregator.modules.webhook.dto.request.TestWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.TestWebhookResponse;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookDeliveryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AppWebhookService {

    AppWebhookResponse getWebhookConfig(UUID userId, UUID appId);

    AppWebhookResponse updateWebhookUrl(UUID userId, UUID appId, UpdateAppWebhookRequest request);

    /** Crée un secret pour la première fois. Erreur si un secret existe déjà. */
    AppWebhookResponse createSecret(UUID userId, UUID appId);

    /** Remplace le secret existant par un nouveau. Erreur si aucun secret n'existe. */
    AppWebhookResponse rotateSecret(UUID userId, UUID appId);

    /** Révoque et supprime le secret. Les webhooks seront envoyés sans signature jusqu'à création d'un nouveau. */
    void revokeSecret(UUID userId, UUID appId);

    TestWebhookResponse testWebhook(UUID userId, UUID appId, TestWebhookRequest request);

    /** Historique paginé des livraisons webhook, filtrable par statut. */
    Page<WebhookDeliveryResponse> listDeliveries(UUID userId, UUID appId, WebhookDeliveryStatus status, Pageable pageable);

    /** Retenvoie un webhook antérieur (crée une nouvelle livraison). */
    WebhookDeliveryResponse replayDelivery(UUID userId, UUID appId, UUID deliveryId);
}
