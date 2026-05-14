package com.sharepay.aggregator.modules.webhook.service;

import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.webhook.dto.request.TestWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.request.UpdateWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.TestWebhookResponse;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookConfigResponse;

import java.util.Map;
import java.util.UUID;

public interface WebhookService {
    WebhookConfigResponse getConfig(UUID apiKeyId);
    WebhookConfigResponse updateConfig(UUID apiKeyId, UpdateWebhookRequest request);

    /** Test via API Key auth (path programmatique). */
    TestWebhookResponse testWebhook(UUID apiKeyId, TestWebhookRequest request);

    /** Test via JWT auth (dashboard merchant) — l'Application est déjà résolue par l'appelant. */
    TestWebhookResponse testWebhookForApp(Application app, TestWebhookRequest request);

    /**
     * Envoie un événement webhook à l'application concernée.
     * Ne lève aucune exception : un échec est loggué sans interrompre le traitement métier.
     */
    void dispatchEvent(Application app, String eventName, Map<String, Object> data);
}
