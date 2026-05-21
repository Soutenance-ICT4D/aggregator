package com.sharepay.aggregator.modules.webhook.controller;

import com.sharepay.aggregator.modules.webhook.dto.request.UpdateWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookConfigResponse;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhook", description = "Configuration et test des webhooks d'application")
@SecurityRequirement(name = "apiKeyAuth")
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @Operation(
            summary = "Consulter la configuration webhook",
            description = "Retourne l'URL et le préfixe masqué du secret webhook pour l'application liée à la clé API. " +
                    "Pour obtenir le secret complet, utilisez la rotation via `POST /api/v1/merchants/apps/{appId}/webhook-secret/rotate`."
    )
    public ApiResponse<WebhookConfigResponse> getConfig(
            @AuthenticationPrincipal String apiKeyId
    ) {
        WebhookConfigResponse response = webhookService.getConfig(UUID.fromString(apiKeyId));
        return ApiResponse.success(response);
    }

    @PatchMapping
    @Operation(
            summary = "Configurer l'URL webhook",
            description = "Met à jour l'URL qui recevra les notifications webhook de l'application. " +
                    "Les événements envoyés sont signés avec HMAC-SHA256 via l'en-tête `X-Sharepay-Signature`."
    )
    public ApiResponse<WebhookConfigResponse> updateConfig(
            @Valid @RequestBody UpdateWebhookRequest request,
            @AuthenticationPrincipal String apiKeyId
    ) {
        WebhookConfigResponse response = webhookService.updateConfig(UUID.fromString(apiKeyId), request);
        return ApiResponse.success("Configuration webhook mise à jour.", response);
    }

}
